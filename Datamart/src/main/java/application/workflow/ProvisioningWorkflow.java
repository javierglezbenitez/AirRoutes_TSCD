
package application.workflow;

import aws.EC2Provisioner;
import application.AppConfig;

public final class ProvisioningWorkflow {
    private ProvisioningWorkflow() {}

    public static InstanceAccess provision(EC2Provisioner ec2, AppConfig cfg) throws Exception {
        String instanceId = ec2.ensureInstance(cfg.getInstanceName());
        String publicIp   = ec2.getPublicIp(instanceId);
        String pemPath    = ec2.getKeyPairPath();
        String user       = System.getenv().getOrDefault("EC2_USER", "ec2-user");
        return new InstanceAccess(instanceId, publicIp, pemPath, user);
    }
}
