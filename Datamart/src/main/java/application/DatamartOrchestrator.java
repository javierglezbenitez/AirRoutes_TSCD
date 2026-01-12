
package application;

import aws.EC2Provisioner;
import application.workflow.*;
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
        InstanceAccess access = ProvisioningWorkflow.provision(ec2, cfg);

        SetupResult setup = SshNeo4jSetupWorkflow.configureNeo4j(
                remote, scriptDeployer, readinessProbe, access, cfg);
        System.out.println("📝 Salida script:\n" + setup.output());

        String boltUri = "bolt://" + access.publicIp() + ":7687";
        Neo4jResources resources = Neo4jClientWorkflow.connect(
                boltUri, cfg.getNeo4jUser(), cfg.getNeo4jPassword(), cfg.getConnectRetrySleepMs());
        GraphRepository repo = new Neo4jGraphRepository(resources.driver());
        repo.ensureSchema();
        DataMartService service = new DataMartServiceImpl(repo);

        try (S3Client s3 = S3Client.builder().region(Region.of(cfg.getRegion())).build()) {
            DatalakeReader reader = new S3DatalakeReaderImpl(s3, cfg.getBucket());
            S3IngestionWorkflow.runForever(reader, service, cfg.getPollIntervalMs());
        } finally {
            resources.client().close();
        }
    }
}
