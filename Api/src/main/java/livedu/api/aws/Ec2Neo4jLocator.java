
package livedu.api.aws;

import org.neo4j.driver.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.*;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Ec2Neo4jLocator {

    public static String autoFindNeo4jBoltHost(String neo4jUser, String neo4jPass, String regionName) {
        try (Ec2Client ec2 = Ec2Client.builder()
                .region(Region.of(regionName))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build()) {

            var req = DescribeInstancesRequest.builder()
                    .filters(Filter.builder().name("instance-state-name").values("running").build())
                    .build();
            var resp = ec2.describeInstances(req);

            List<Instance> candidates = new ArrayList<>();
            resp.reservations().forEach(r -> r.instances().forEach(i -> {
                if (i.publicIpAddress() != null && !i.publicIpAddress().isBlank()) {
                    candidates.add(i);
                }
            }));

            candidates.sort(Comparator.comparing(Instance::launchTime,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());

            for (var inst : candidates) {
                String ip = inst.publicIpAddress();
                System.out.println("   • Probar bolt://" + ip + ":7687");
                if (canConnectBolt(ip, neo4jUser, neo4jPass)) {
                    System.out.println("✔ Neo4j detectado en " + ip + " (" + regionName + ")");
                    return ip;
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ Fallo autodiscovery AWS: " + e.getMessage());
        }
        return null;
    }

    private static boolean canConnectBolt(String ip, String user, String pass) {
        String uri = "bolt://" + ip + ":7687";
        Config cfg = Config.builder()
                .withConnectionTimeout(5, TimeUnit.SECONDS)
                .build();
        try (var driver = GraphDatabase.driver(uri, AuthTokens.basic(user, pass), cfg);
             var session = driver.session()) {
            session.run("RETURN 1").consume();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
