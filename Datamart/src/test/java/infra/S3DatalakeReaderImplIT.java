package infra;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class S3DatalakeReaderImplIT {

    private static final DockerImageName IMG = DockerImageName.parse("localstack/localstack:2.3");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(IMG)
            .withServices(LocalStackContainer.Service.S3);

    static S3Client s3;
    static String bucket = "datamart-ci";

    @BeforeAll
    static void setup() {
        s3 = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test","test")))
                .build();
        s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
    }

    @AfterAll
    static void tearDown() {
        s3.close();
    }

    @Test
    void listAndRead_shouldFindJsonAndParse() throws Exception {
        String today = LocalDate.now().toString();
        // Subimos dos objetos: uno como array y otro como { routes: [...] }
        String key1 = "datalake/" + today + "/arr.json";
        String key2 = "datalake/" + today + "/routes.json";

        String arrJson = """
      [
        {"codigoVuelo":"S3-1","origen":"MAD","destino":"JFK","duracionMinutos":100,"precio":100.0,"aerolinea":"Iberia","timestamp":1,"escala":"None","embarque":3}
      ]""";
        String routesJson = """
      {"routes":[
        {"codigoVuelo":"S3-2","origen":"CDG","destino":"MAD","duracionMinutos":90,"precio":50.0,"aerolinea":"LATAM","timestamp":2,"escala":"JFK","embarque":2}
      ]}
      """;

        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key1).contentType("application/json").build(),
                RequestBody.fromString(arrJson));
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key2).contentType("application/json").build(),
                RequestBody.fromString(routesJson));

        S3DatalakeReaderImpl reader = new S3DatalakeReaderImpl(s3, bucket);

        var keys = reader.listFilesForDate(LocalDate.now());
        assertTrue(keys.contains(key1));
        assertTrue(keys.contains(key2));

        var data = reader.readSpecificKeys(List.of(key1, key2));
        assertEquals(2, data.size());
        assertEquals("S3-1", data.get(0).get("codigoVuelo"));
    }
}