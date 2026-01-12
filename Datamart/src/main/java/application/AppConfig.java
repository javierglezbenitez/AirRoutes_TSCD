
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

    private final String datamartMode;
    private final String neo4jBoltUri;

    public AppConfig(Map<String, String> env) {
        this.region = env.get("AWS_REGION");
        this.bucket = env.get("S3_BUCKET");
        this.neo4jUser = env.get("NEO4J_USER");
        this.neo4jPassword = Objects.requireNonNull(env.get("NEO4J_PASSWORD"), "NEO4J_PASSWORD requerido");
        this.instanceName = env.get("EC2_INSTANCE_NAME");
        this.sshTimeoutMs = 180_000L;
        this.connectRetrySleepMs = 10_000L;
        this.pollIntervalMs = 40_000L;

        this.datamartMode = env.get("DATAMART_MODE");

        String bolt = env.get("NEO4J_BOLT_URI");
        if (bolt == null || bolt.isBlank()) {
            bolt = env.get("NEO4J_URI");
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
