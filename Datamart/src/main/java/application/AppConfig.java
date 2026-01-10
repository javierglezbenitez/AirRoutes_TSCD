
package application;

import java.util.Map;
import java.util.Objects;

public class AppConfig {
    private final String region;
    private final String bucket;
    private final String neo4jUser;
    private final String neo4jPassword;
    private final String instanceName;
    private final long sshTimeoutMs;
    private final long connectRetrySleepMs;
    private final long pollIntervalMs;

    private final String datamartMode;   // "EC2" (por defecto) o "LOCAL"
    private final String neo4jBoltUri;   // soporta NEO4J_BOLT_URI o NEO4J_URI

    public AppConfig(Map<String, String> env) {
        this.region = env.getOrDefault("AWS_REGION", "");
        this.bucket = env.getOrDefault("S3_BUCKET", "");
        this.neo4jUser = env.getOrDefault("NEO4J_USER", "");
        this.neo4jPassword = Objects.requireNonNull(env.get("NEO4J_PASSWORD"), "NEO4J_PASSWORD requerido");
        this.instanceName = env.getOrDefault("EC2_INSTANCE_NAME", "");
        this.sshTimeoutMs = 180_000L;
        this.connectRetrySleepMs = 10_000L;
        this.pollIntervalMs = 40_000L;

        // ✅ Default a EC2 si no está definido
        this.datamartMode = env.getOrDefault("DATAMART_MODE", "EC2");

        // ✅ Soporta ambas variables, priorizando NEO4J_BOLT_URI
        String bolt = env.getOrDefault("NEO4J_BOLT_URI", "");
        if (bolt == null || bolt.isBlank()) {
            bolt = env.getOrDefault("NEO4J_URI", "bolt://localhost:7687");
        }
        this.neo4jBoltUri = bolt;
    }

    public String getRegion() { return region; }
    public String getBucket() { return bucket; }
    public String getNeo4jUser() { return neo4jUser; }
    public String getNeo4jPassword() { return neo4jPassword; }
    public String getInstanceName() { return instanceName; }
    public long getSshTimeoutMs() { return sshTimeoutMs; }
    public long getConnectRetrySleepMs() { return connectRetrySleepMs; }
    public long getPollIntervalMs() { return pollIntervalMs; }

    public String getDatamartMode() { return datamartMode; }
    public String getNeo4jBoltUri() { return neo4jBoltUri; }
}
