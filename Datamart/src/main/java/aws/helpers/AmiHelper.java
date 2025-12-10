
package aws.helpers;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

public final class AmiHelper {
    private AmiHelper() {}

    public static String latestAmazonLinux2Ami(Ec2Client ec2) {
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
