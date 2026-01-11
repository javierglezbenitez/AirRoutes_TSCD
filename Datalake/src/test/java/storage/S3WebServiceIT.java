
package storage;

import architecture.AirRoute;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;  // 👈 IMPORTANTE
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class S3WebServiceIT {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:2.3");

    @Container
    static LocalStackContainer localstack =
            new LocalStackContainer(LOCALSTACK_IMAGE)
                    .withServices(LocalStackContainer.Service.S3);

    static S3Client s3;
    static String bucket = "routes-datalake-ci";

    @BeforeAll
    static void setup() {
        s3 = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .build();
    }

    @AfterAll
    static void tearDown() {
        s3.close();
    }

    @Test
    void saveAirRoutes_shouldCreateBucketIfMissingAndStoreJsonList() {
        S3WebService service = new S3WebService(s3, bucket);

        List<AirRoute> routes = List.of(
                new AirRoute("FL-1","MAD","JFK",100,100.0,"Iberia",System.currentTimeMillis(),"None",3),
                new AirRoute("FL-2","CDG","MAD",90,50.0,"LATAM",System.currentTimeMillis(),"JFK",2)
        );

        String todayFolder = "datalake/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        service.saveAirRoutes(routes);

        var list = s3.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(todayFolder + "/")
                .build());

        assertTrue(list.hasContents(), "No se encontró ningún objeto en el prefijo del día");
    }
}
