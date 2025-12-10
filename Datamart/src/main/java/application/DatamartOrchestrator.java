
package application;

import aws.EC2Provisioner;
import config.Neo4jClientProvider;
import config.Neo4jClientFactory;
import infra.DatalakeReader;
import infra.S3DatalakeReaderImpl;
import remote.RemoteExecutor;
import remote.SSHRemoteExecutor;
import repository.GraphRepository;
import repository.Neo4jGraphRepository;
import service.DataMartService;
import service.DataMartServiceImpl;
import setup.*;

import org.neo4j.driver.Driver;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.util.*;

public class DatamartOrchestrator {
    private final AppConfig cfg;
    private final EC2Provisioner ec2;
    private final RemoteExecutor remote;
    private final ScriptDeployer scriptDeployer;
    private final ReadinessProbe readinessProbe;

    public DatamartOrchestrator(AppConfig cfg) {
        this.cfg = cfg;
        this.ec2 = new EC2Provisioner(Region.of(cfg.getRegion()));
        this.remote = new SSHRemoteExecutor();
        this.scriptDeployer = new ScriptDeployerImpl(remote, new SetupScriptRepository());
        this.readinessProbe = new Neo4jReadinessProbe(remote);
    }

    public void run() throws Exception {
        // 1) EC2
        String instanceId = ec2.ensureInstance(cfg.getInstanceName());
        String publicIp = ec2.getPublicIp(instanceId);
        String pemPath = ec2.getKeyPairPath();

        // 2) SSH readiness
        remote.waitForSSH(publicIp, "ec2-user", pemPath, cfg.getSshTimeoutMs());

        // 3) Setup Neo4j (script)
        String output = scriptDeployer.deployAndRun(publicIp, "ec2-user", pemPath,
                cfg.getNeo4jUser(), cfg.getNeo4jPassword(), publicIp);
        System.out.println("📝 Salida script:\n" + output);

        // 4) Readiness (Bolt)
        boolean ready = readinessProbe.waitForBolt(publicIp, "ec2-user", pemPath, 30, 5000);
        if (!ready) System.err.println("❌ Neo4j no listo. Continuando con reintentos del driver.");

        // 5) URLs
        String boltUri = "bolt://" + publicIp + ":7687";

        // 6) Conectar con reintento
        Neo4jClientProvider neo = Neo4jClientFactory.connectWithRetry(boltUri, cfg.getNeo4jUser(), cfg.getNeo4jPassword(), cfg.getConnectRetrySleepMs());
        Driver driver = neo.getDriver();
        GraphRepository repo = new Neo4jGraphRepository(driver);
        repo.ensureSchema();
        DataMartService service = new DataMartServiceImpl(repo);

        // 7) Polling S3
        try (S3Client s3 = S3Client.builder().region(Region.of(cfg.getRegion())).build()) {
            DatalakeReader reader = new S3DatalakeReaderImpl(s3, cfg.getBucket());
            Set<String> processedKeys = new HashSet<>();
            LocalDate currentDate = LocalDate.now();

            System.out.println("📡 Ingesta continua. Día actual: " + currentDate);
            while (true) {
                try {
                    LocalDate now = LocalDate.now();
                    if (!now.equals(currentDate)) {
                        System.out.println("🔄 Cambio de día (" + currentDate + " -> " + now + "). Reseteando estado...");
                        currentDate = now;
                        processedKeys.clear();
                    }

                    List<String> keys = reader.listFilesForDate(currentDate);
                    List<String> newKeys = keys.stream().filter(k -> !processedKeys.contains(k)).toList();

                    if (newKeys.isEmpty()) {
                        System.out.println("😴 No hay nuevos archivos. Esperando " + cfg.getPollIntervalMs()/1000 + "s...");
                    } else {
                        System.out.println("➕ Nuevos archivos: " + newKeys.size());
                        List<Map<String, Object>> routes = reader.readSpecificKeys(newKeys);
                        service.upsertToday(routes);
                        processedKeys.addAll(newKeys);
                        System.out.println("✔ Marcadas como procesadas (" + processedKeys.size() + " total)");
                    }
                    Thread.sleep(cfg.getPollIntervalMs());
                } catch (Exception e) {
                    System.err.println("❌ Error en ciclo de ingesta: " + e.getMessage());
                    Thread.sleep(Math.max(20000, cfg.getPollIntervalMs()/2));
                }
            }
        } finally {
            neo.close(); // shutdown ordenado
        }
    }
}
