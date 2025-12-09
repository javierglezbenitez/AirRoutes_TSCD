
package application;

import architecture.StorageFactory;
import architecture.Storage;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import storage.LocalStorage;
import storage.S3WebService;

public class DefaultStorageFactory implements StorageFactory {

    @Override
    public Storage createStorage(String mode) {
        if ("S3".equalsIgnoreCase(mode)) {
            S3Client s3Client = S3Client.builder()
                    .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1")))
                    .build();

            String bucketName = System.getenv().getOrDefault("S3_BUCKET", "airroutes-datalake");
            return new S3WebService(s3Client, bucketName);
        } else {
            return new LocalStorage("storage");
        }
    }
}
