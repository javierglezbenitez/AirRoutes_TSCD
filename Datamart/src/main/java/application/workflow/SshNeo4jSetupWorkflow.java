
package application.workflow;

import remote.RemoteExecutor;
import setup.ReadinessProbe;
import setup.ScriptDeployer;
import application.AppConfig;

public final class SshNeo4jSetupWorkflow {
    private SshNeo4jSetupWorkflow() {}

    public static SetupResult configureNeo4j(
            RemoteExecutor remote,
            ScriptDeployer scriptDeployer,
            ReadinessProbe readinessProbe,
            InstanceAccess access,
            AppConfig cfg
    ) throws Exception {
        remote.waitForSSH(access.publicIp(), access.user(), access.pemPath(), cfg.getSshTimeoutMs());

        String output = scriptDeployer.deployAndRun(
                access.publicIp(), access.user(), access.pemPath(),
                cfg.getNeo4jUser(), cfg.getNeo4jPassword(), access.publicIp());

        boolean ready = readinessProbe.waitForBolt(access.publicIp(), access.user(), access.pemPath(), 30, 5000);
        if (!ready) System.err.println("❌ Neo4j no listo. Continuando con reintentos del driver.");

        return new SetupResult(ready, output);
    }
}
