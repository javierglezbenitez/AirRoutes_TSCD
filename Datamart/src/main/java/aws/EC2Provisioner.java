
package aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class EC2Provisioner implements InstanceProvisioner {
    private final Ec2Client ec2;
    private String keyPairPath;
    private String keyPairName;

    public EC2Provisioner(Region region) {
        this.ec2 = Ec2Client.builder().region(region).build();
        System.out.println("🔧 EC2Provisioner inicializado para región " + region);
    }

    @Override
    public String ensureInstance(String name) throws Exception {
        System.out.println("🔎 Buscando instancias RUNNING con tag Name=" + name + "...");
        List<Instance> instances = ec2.describeInstances().reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name() == InstanceStateName.RUNNING)
                .filter(i -> i.tags().stream().anyMatch(t -> "Name".equals(t.key()) && name.equals(t.value())))
                .toList();

        if (!instances.isEmpty()) {
            Instance instance = instances.get(0);
            System.out.println("ℹ️ Instancia existente encontrada: " + instance.instanceId());

            // ✅ Resolver keyPairName de la instancia y la ruta del PEM local
            this.keyPairName = instance.keyName();
            this.keyPairPath = resolvePemPathForKeyName(this.keyPairName);
            if (this.keyPairPath == null || this.keyPairPath.isBlank()) {
                // Si no se encuentra, intenta usar la ruta del ENV de todas formas
                String pemPathEnv = System.getenv().getOrDefault(
                        "EC2_KEY_PATH",
                        System.getProperty("user.home") + "/.ssh/" + this.keyPairName + ".pem"
                );
                File pemFile = new File(pemPathEnv);
                if (!pemFile.exists()) {
                    throw new IllegalStateException(
                            "No se encontró el .pem local para keyName=" + this.keyPairName +
                                    ". Coloca el archivo en: " + pemPathEnv + " o ajusta EC2_KEY_PATH."
                    );
                }
                this.keyPairPath = pemFile.getAbsolutePath();
            }

            return instance.instanceId();
        }

        // No hay instancia RUNNING: crear keypair/sg/ami y lanzar
        createKeyPair(); // esta versión NO recrea si ya existe, y usa EC2_KEY_PATH
        String sgId = createSecurityGroup();
        String ami = getLatestAmazonLinux2AMI();

        System.out.println("🚀 Lanzando EC2 t2.micro con AMI " + ami + "...");
        RunInstancesResponse run = ec2.runInstances(RunInstancesRequest.builder()
                .imageId(ami)
                .instanceType(InstanceType.T2_MICRO)
                .keyName(keyPairName)
                .securityGroupIds(sgId)
                .minCount(1)
                .maxCount(1)
                .tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder().key("Name").value(name).build())
                        .build())
                .build());

        String instanceId = run.instances().get(0).instanceId();
        System.out.println("   ✔ Instancia EC2 creada: " + instanceId);
        Thread.sleep(15_000);
        return instanceId;
    }

    @Override
    public String getPublicIp(String instanceId) {
        System.out.println("🌐 Obteniendo IP pública de " + instanceId + "...");
        DescribeInstancesResponse resp = ec2.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(instanceId).build());
        String ip = resp.reservations().get(0).instances().get(0).publicIpAddress();
        System.out.println("   ✔ IP: " + ip);
        return ip;
    }

    @Override
    public String getKeyPairPath() {
        return keyPairPath;
    }

    /* --- privados --- */

    /**
     * Crea el key pair si no existe en AWS.
     * Si ya existe, NO lo recrea: solo resuelve la ruta local del PEM desde EC2_KEY_PATH
     * o desde ~/.ssh/<keyName>.pem.
     */
    private void createKeyPair() throws Exception {
        System.out.println("🔑 Creando/Resolviendo KeyPair...");

        // Lee nombre de la clave desde ENV (o usa 'datamart-key')
        keyPairName = System.getenv().getOrDefault("EC2_KEY_NAME", "datamart-key");

        // Comprueba si el key pair ya existe en AWS
        boolean existsInAws = ec2.describeKeyPairs().keyPairs().stream()
                .anyMatch(k -> keyPairName.equals(k.keyName()));

        // Ruta objetivo del PEM (desde ENV o por defecto en ~/.ssh)
        String pemPathEnv = System.getenv().getOrDefault(
                "EC2_KEY_PATH",
                System.getProperty("user.home") + "/.ssh/" + keyPairName + ".pem"
        );
        File pemFile = new File(pemPathEnv);

        if (existsInAws) {
            System.out.println("   ✔ KeyPair ya existe en AWS: " + keyPairName);
            // Si ya existe en AWS, NO se puede descargar de nuevo. Debe existir en local.
            if (!pemFile.exists()) {
                throw new IllegalStateException(
                        "El KeyPair '" + keyPairName + "' ya existe en AWS, pero falta el PEM local en: " +
                                pemFile.getAbsolutePath() + ". Debes tener ese .pem del momento de la creación."
                );
            }
            ensurePemPermissions(pemFile);
            keyPairPath = pemFile.getAbsolutePath();
            System.out.println("   ✔ Usando PEM existente: " + keyPairPath);
            return;
        }

        // Si no existe en AWS, lo creamos y guardamos el PEM en la ruta indicada
        CreateKeyPairResponse keyPair = ec2.createKeyPair(CreateKeyPairRequest.builder()
                .keyName(keyPairName).build());
        byte[] pemBytes = keyPair.keyMaterial().getBytes();

        File parentDir = pemFile.getParentFile();
        if (!parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("No se pudo crear el directorio: " + parentDir.getAbsolutePath());
        }

        try (FileOutputStream fos = new FileOutputStream(pemFile)) {
            fos.write(pemBytes);
        }

        ensurePemPermissions(pemFile);
        keyPairPath = pemFile.getAbsolutePath();
        System.out.println("   ✔ KeyPair guardado en: " + keyPairPath);
    }

    private void ensurePemPermissions(File pemFile) {
        pemFile.setReadable(true, true);
        pemFile.setExecutable(false);
        pemFile.setWritable(true, true);
    }

    /**
     * Intenta resolver la ruta local del PEM para un keyName dado:
     * 1) Si EC2_KEY_NAME coincide y hay EC2_KEY_PATH -> usa EC2_KEY_PATH
     * 2) Si existe ~/.ssh/<keyName>.pem -> úsalo
     * 3) Si no, devuelve null
     */
    private String resolvePemPathForKeyName(String keyName) {
        String envKeyName = System.getenv().getOrDefault("EC2_KEY_NAME", "");
        String envKeyPath = System.getenv().getOrDefault("EC2_KEY_PATH", "");

        if (!envKeyName.isBlank() && keyName.equals(envKeyName) && !envKeyPath.isBlank()) {
            File f = new File(envKeyPath);
            if (f.exists()) return f.getAbsolutePath();
        }

        String homeCandidate = System.getProperty("user.home") + "/.ssh/" + keyName + ".pem";
        File f2 = new File(homeCandidate);
        if (f2.exists()) return f2.getAbsolutePath();

        return null;
    }

    private String createSecurityGroup() {
        System.out.println("🛡️  Creando Security Group con puertos 22/7474/7687...");
        CreateSecurityGroupResponse sg = ec2.createSecurityGroup(CreateSecurityGroupRequest.builder()
                .groupName("datamart-sg-" + System.currentTimeMillis())
                .description("SG para Datamart Neo4j")
                .build());
        String sgId = sg.groupId();

        IpPermission ssh = IpPermission.builder().ipProtocol("tcp").fromPort(22).toPort(22)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build()).build();
        IpPermission httpNeo = IpPermission.builder().ipProtocol("tcp").fromPort(7474).toPort(7474)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build()).build();
        IpPermission boltNeo = IpPermission.builder().ipProtocol("tcp").fromPort(7687).toPort(7687)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build()).build();

        ec2.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(sgId).ipPermissions(ssh, httpNeo, boltNeo).build());
        System.out.println("   ✔ Security Group creado: " + sgId);
        return sgId;
    }

    private String getLatestAmazonLinux2AMI() {
        System.out.println("🔎 Buscando AMI Amazon Linux 2 más reciente...");
        DescribeImagesResponse response = ec2.describeImages(DescribeImagesRequest.builder()
                .owners("amazon")
                .filters(
                        Filter.builder().name("name").values("amzn2-ami-hvm-*-x86_64-gp2").build(),
                        Filter.builder().name("state").values("available").build()
                ).build());

        String ami = response.images().stream()
                .max((i1, i2) -> i1.creationDate().compareTo(i2.creationDate()))
                .orElseThrow(() -> new RuntimeException("No se encontró Amazon Linux 2"))
                .imageId();

        System.out.println("   ✔ AMI: " + ami);
        return ami;
    }
}
