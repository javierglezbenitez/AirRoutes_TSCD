
package aws.helpers;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public final class InstanceHelper {
    private InstanceHelper() {}

    public static Instance findRunningByName(Ec2Client ec2, String name) {
        return ec2.describeInstances().reservations().stream()
                .flatMap(r -> r.instances().stream())
                .filter(i -> i.state().name() == InstanceStateName.RUNNING)
                .filter(i -> i.tags().stream().anyMatch(t -> "Name".equals(t.key()) && name.equals(t.value())))
                .findFirst().orElse(null);
    }

    public static String launchT2Micro(Ec2Client ec2, String ami, String sgId, String keyName, String nameTag) {
        RunInstancesResponse run = ec2.runInstances(RunInstancesRequest.builder()
                .imageId(ami)
                .instanceType(InstanceType.T2_MICRO)
                .keyName(keyName)
                .securityGroupIds(sgId)
                .minCount(1).maxCount(1)
                .tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder().key("Name").value(nameTag).build())
                        .build())
                .build());
        return run.instances().get(0).instanceId();
    }

    public static String getPublicIp(Ec2Client ec2, String instanceId) {
        DescribeInstancesResponse resp = ec2.describeInstances(DescribeInstancesRequest.builder()
                .instanceIds(instanceId).build());
        return resp.reservations().get(0).instances().get(0).publicIpAddress();
    }
}
