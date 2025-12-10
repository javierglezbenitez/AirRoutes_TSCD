
package aws;

import aws.helpers.AmiHelper;
import aws.helpers.KeyPairHelper;
import aws.helpers.SecurityGroupHelper;
import aws.helpers.InstanceHelper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class EC2Provisioner implements InstanceProvisioner {
    private final Ec2Client ec2;
    private String keyPairPath, keyPairName;

    public EC2Provisioner(Region region) {
        this.ec2 = Ec2Client.builder().region(region).build();
        System.out.println("🔧 EC2Provisioner inicializado para región " + region);
    }

    @Override
    public String ensureInstance(String name) throws Exception {
        var inst = InstanceHelper.findRunningByName(ec2, name);
        if (inst != null) {
            keyPairName = inst.keyName();
            keyPairPath = KeyPairHelper.resolvePemByKeyName(keyPairName);
            if (keyPairPath == null || keyPairPath.isBlank())
                keyPairPath = KeyPairHelper.resolvePemFromEnvOrHome(keyPairName);
            System.out.println("ℹ️ Instancia existente encontrada: " + inst.instanceId());
            return inst.instanceId();
        }
        keyPairName = System.getenv().getOrDefault("EC2_KEY_NAME", "datamart-key");
        keyPairPath = KeyPairHelper.resolveOrCreatePem(
                ec2, keyPairName,
                System.getenv().getOrDefault("EC2_KEY_PATH",
                        System.getProperty("user.home") + "/.ssh/" + keyPairName + ".pem"));
        String sgId = SecurityGroupHelper.createDefaultSg(ec2);
        String ami  = AmiHelper.latestAmazonLinux2Ami(ec2);
        String id   = InstanceHelper.launchT2Micro(ec2, ami, sgId, keyPairName, name);
        System.out.println("   ✔ Instancia EC2 creada: " + id);
        Thread.sleep(15_000);
        return id;
    }

    @Override public String getPublicIp(String instanceId) { return InstanceHelper.getPublicIp(ec2, instanceId); }
    @Override public String getKeyPairPath() { return keyPairPath; }
}
