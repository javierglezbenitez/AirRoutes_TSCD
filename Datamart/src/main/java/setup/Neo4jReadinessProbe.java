
package setup;

import remote.RemoteExecutor;

public class Neo4jReadinessProbe implements ReadinessProbe {
    private final RemoteExecutor remote;

    public Neo4jReadinessProbe(RemoteExecutor remote) { this.remote = remote; }

    @Override
    public boolean waitForBolt(String host, String user, String pemPath, int attempts, long sleepMs) throws Exception {
        for (int i = 1; i <= attempts; i++) {
            System.out.println("🩺 Chequeo readiness (" + i + "/" + attempts + ")...");
            String ps = remote.run(host, user, pemPath,
                    "sudo docker ps --format '{{.Names}} {{.Status}} {{.Ports}}' | grep -w neo4j-datamart || true");
            System.out.println(" docker ps: " + (ps.isBlank() ? "<vacio>" : ps.trim()));

            String ss = remote.run(host, user, pemPath, "sudo ss -ltn | grep ':7687' || true");
            boolean boltListening = !ss.isBlank();
            System.out.println(" bolt listening? " + boltListening);
            if (boltListening) return true;

            Thread.sleep(sleepMs);
        }
        return false;
    }
}
