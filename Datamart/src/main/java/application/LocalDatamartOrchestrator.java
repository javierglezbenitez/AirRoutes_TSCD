
package application;

import application.workflow.Neo4jClientWorkflow;
import application.workflow.Neo4jResources;
import application.workflow.S3IngestionWorkflow;
import infra.DatalakeReader;
import infra.S3DatalakeReaderImpl;
import infra.LocalDatalakeReaderImpl;
import infra.FallbackDatalakeReader;
import repository.GraphRepository;
import repository.Neo4jGraphRepository;
import service.DataMartService;
import service.DataMartServiceImpl;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class LocalDatamartOrchestrator {
    private final AppConfig cfg;

    public LocalDatamartOrchestrator(AppConfig cfg) { this.cfg = cfg; }

    public void run() throws Exception {
        final String boltUri = cfg.getNeo4jBoltUri();
        Neo4jResources resources = Neo4jClientWorkflow.connect(
                boltUri, cfg.getNeo4jUser(), cfg.getNeo4jPassword(), cfg.getConnectRetrySleepMs());

        try {
            GraphRepository repo = new Neo4jGraphRepository(resources.driver());
            repo.ensureSchema();
            DataMartService service = new DataMartServiceImpl(repo);

            String mode = System.getenv().getOrDefault("DATALAKE_MODE", "S3").toUpperCase();
            String baseDir = System.getenv().getOrDefault("DATALAKE_BASE_DIR", ".");
            System.out.println("⚙️  DATALAKE_MODE=" + mode + " | DATALAKE_BASE_DIR=" + baseDir);

            switch (mode) {
                case "LOCAL": {
                    DatalakeReader reader = new LocalDatalakeReaderImpl(baseDir);
                    System.out.printf("🚚 Iniciando ingesta desde DATALAKE LOCAL cada %d ms%n", cfg.getPollIntervalMs());
                    S3IngestionWorkflow.runForever(reader, service, cfg.getPollIntervalMs());
                    break;
                }
                case "S3": {
                    try (S3Client s3 = S3Client.builder().region(Region.of(cfg.getRegion())).build()) {
                        DatalakeReader s3Reader = new S3DatalakeReaderImpl(s3, cfg.getBucket());
                        DatalakeReader localReader = new LocalDatalakeReaderImpl(baseDir);
                        DatalakeReader reader = new FallbackDatalakeReader(s3Reader, localReader);
                        System.out.printf("🚚 Iniciando ingesta desde S3 (bucket=%s) con fallback a LOCAL cada %d ms%n",
                                cfg.getBucket(), cfg.getPollIntervalMs());
                        S3IngestionWorkflow.runForever(reader, service, cfg.getPollIntervalMs());
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("DATALAKE_MODE desconocido: " + mode + " (usa S3 o LOCAL)");
            }
        } finally {
            resources.client().close(); // ✅ cierre ordenado también en LOCAL
        }
    }
}
