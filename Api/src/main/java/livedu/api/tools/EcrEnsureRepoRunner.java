
package livedu.api.tools;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ecr.EcrClient;
import software.amazon.awssdk.services.ecr.model.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

/**
 * Ejecuta esta clase para asegurar que exista el repositorio ECR y te devuelva su URI.
 * Lee variables de entorno: AWS_REGION, ECR_REPO_NAME.
 */
public class EcrEnsureRepoRunner {

    public static void main(String[] args) {
        String regionName = getenvOrDefault("AWS_REGION", "us-east-1");
        String repoName   = getenvOrDefault("ECR_REPO_NAME", "graph-routes-api");

        Region region = Region.of(regionName);

        try (EcrClient ecr = EcrClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
             StsClient sts = StsClient.builder()
                     .region(region)
                     .credentialsProvider(DefaultCredentialsProvider.create())
                     .build()) {

            // ¿Existe el repo?
            boolean exists = true;
            try {
                ecr.describeRepositories(DescribeRepositoriesRequest.builder()
                        .repositoryNames(repoName).build());
                System.out.println("✔ ECR repo ya existe: " + repoName);
            } catch (RepositoryNotFoundException e) {
                exists = false;
            }

            // Si no existe, crearlo
            if (!exists) {
                System.out.println("ℹ️ Creando repositorio ECR: " + repoName);
                ecr.createRepository(CreateRepositoryRequest.builder()
                        .repositoryName(repoName)
                        .imageTagMutability(ImageTagMutability.MUTABLE)
                        .imageScanningConfiguration(ImageScanningConfiguration.builder()
                                .scanOnPush(false).build())
                        .build());
                System.out.println("✔ ECR repo creado: " + repoName);
            }

            // Construir la URI del repo
            GetCallerIdentityResponse ident = sts.getCallerIdentity();
            String accountId = ident.account();
            String repoUri = accountId + ".dkr.ecr." + region.id() + ".amazonaws.com/" + repoName;

            // Imprimir en formato fácil de capturar
            System.out.println("ECR_REPO_URI=" + repoUri);
        } catch (EcrException e) {
            System.err.println("❌ Error ECR: " + e.awsErrorDetails().errorMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("❌ Error general: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
