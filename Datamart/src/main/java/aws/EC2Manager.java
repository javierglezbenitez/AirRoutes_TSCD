
package aws;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.regions.Region;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class EC2Manager {
    private final Ec2Client ec2;
    private String keyPairPath;
    private String keyPairName;

    public EC2Manager(Region region) {
        this.ec2 = Ec2Client.builder()
                .region(region)
                .build();
        System.out.println("🔧 EC2Manager inicializado para región " + region);
    }

    public String getKeyPairPath() {
        return keyPairPath;
    }

    public String getLatestAmazonLinux2AMI() {
        System.out.println("🔎 Buscando AMI Amazon Linux 2 más reciente...");
        DescribeImagesResponse response = ec2.describeImages(DescribeImagesRequest.builder()
                .owners("amazon")
                .filters(
                        Filter.builder().name("name").values("amzn2-ami-hvm-*-x86_64-gp2").build(),
                        Filter.builder().name("state").values("available").build()
                )
                .build());

        String ami = response.images().stream()
                .max((i1, i2) -> i1.creationDate().compareTo(i2.creationDate()))
                .orElseThrow(() -> new RuntimeException("No se encontró Amazon Linux 2"))
                .imageId();

        System.out.println("   ✔ AMI: " + ami);
        return ami;
    }

    private void createKeyPair() throws Exception {
        System.out.println("🔑 Creando KeyPair temporal...");
        keyPairName = "datamart-key-" + System.currentTimeMillis();
        CreateKeyPairResponse keyPair = ec2.createKeyPair(CreateKeyPairRequest.builder().keyName(keyPairName).build());
        byte[] pemBytes = keyPair.keyMaterial().getBytes();

        File tempPem = File.createTempFile("datamart-key-", ".pem");
        try (FileOutputStream fos = new FileOutputStream(tempPem)) {
            fos.write(pemBytes);
        }
        tempPem.setReadable(true, true);
        tempPem.setExecutable(false);
        tempPem.setWritable(true, true);

        keyPairPath = tempPem.getAbsolutePath();
        System.out.println("   ✔ KeyPair guardado en: " + keyPairPath);
    }

    private String createSecurityGroup() {
        System.out.println("🛡️  Creando Security Group con puertos 22/7474/7687...");
        CreateSecurityGroupResponse sg = ec2.createSecurityGroup(CreateSecurityGroupRequest.builder()
                .groupName("datamart-sg-" + System.currentTimeMillis())
                .description("SG para Datamart Neo4j")
                .build());

        String sgId = sg.groupId();

        IpPermission ssh = IpPermission.builder()
                .ipProtocol("tcp").fromPort(22).toPort(22)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                .build();

        IpPermission httpNeo = IpPermission.builder()
                .ipProtocol("tcp").fromPort(7474).toPort(7474)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                .build();

        IpPermission boltNeo = IpPermission.builder()
                .ipProtocol("tcp").fromPort(7687).toPort(7687)
                .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                .build();

        ec2.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder()
                .groupId(sgId)
                .ipPermissions(ssh, httpNeo, boltNeo)
                .build());

        System.out.println("   ✔ Security Group creado: " + sgId);
        return sgId;
    }

    public String getOrCreateInstance(String name) throws Exception {
        System.out.println("🔎 Buscando instancias RUNNING con tag Name=" + name + "...");
        List<Instance> instances = ec2.describeInstances().reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name() == InstanceStateName.RUNNING)
                .filter(i -> i.tags().stream().anyMatch(t -> t.key().equals("Name") && t.value().equals(name)))
                .toList();

        if (!instances.isEmpty()) {
            Instance instance = instances.get(0);
            System.out.println("ℹ️ Instancia existente encontrada: " + instance.instanceId());
            return instance.instanceId();
        }

        System.out.println("🆕 No existe instancia; creando KeyPair y SG...");
        createKeyPair();
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

        System.out.println("⏳ Esperando asignación de IP pública (15s)...");
        Thread.sleep(15_000);

        return instanceId;
    }

    public String getInstancePublicIp(String instanceId) {
        System.out.println("🌐 Obteniendo IP pública de " + instanceId + "...");
        DescribeInstancesResponse resp = ec2.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(instanceId).build());
        String ip = resp.reservations().get(0).instances().get(0).publicIpAddress();
        System.out.println("   ✔ IP: " + ip);
        return ip;
    }
}
