
package livedu.api.aws;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public class Ec2TagLocator {


    public static String findPublicIpByNameTag(String datamartTag, String regionName) {
        try (Ec2Client ec2 = Ec2Client.builder()
                .region(Region.of(regionName))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            DescribeInstancesRequest req = DescribeInstancesRequest.builder()
                    .filters(
                            Filter.builder().name("instance-state-name").values("running").build(),
                            Filter.builder().name("tag:Name").values(datamartTag).build()
                    )
                    .build();

            DescribeInstancesResponse resp = ec2.describeInstances(req);
            for (Reservation res : resp.reservations()) {
                for (Instance inst : res.instances()) {
                    String ip = inst.publicIpAddress();
                    if (ip != null && !ip.isBlank()) {
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Error resolviendo IP por tag '" + datamartTag + "': " + e.getMessage());
        }
        return null;
    }
}
