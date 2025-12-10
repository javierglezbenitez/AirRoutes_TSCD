
package aws.helpers;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public final class SecurityGroupHelper {
    private SecurityGroupHelper() {}

    public static String createDefaultSg(Ec2Client ec2) {
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
}
