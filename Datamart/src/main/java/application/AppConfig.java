
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

    public AppConfig(Map<String, String> env) {
        this.region = env.getOrDefault("AWS_REGION", "us-east-1");
        this.bucket = env.getOrDefault("S3_BUCKET", "airroutes-datalake");
        this.neo4jUser = env.getOrDefault("NEO4J_USER", "neo4j");
        this.neo4jPassword = Objects.requireNonNull(env.get("NEO4J_PASSWORD"), "NEO4J_PASSWORD requerido");
        this.instanceName = env.getOrDefault("EC2_INSTANCE_NAME", "neo4j-datamart");
        this.sshTimeoutMs = Long.parseLong(env.getOrDefault("SSH_TIMEOUT_MS", "180000"));
        this.connectRetrySleepMs = Long.parseLong(env.getOrDefault("NEO4J_RETRY_SLEEP_MS", "10000"));
        this.pollIntervalMs = Long.parseLong(env.getOrDefault("POLL_INTERVAL_MS", "40000"));
    }

    public String getRegion() { return region; }
    public String getBucket() { return bucket; }
    public String getNeo4jUser() { return neo4jUser; }
    public String getNeo4jPassword() { return neo4jPassword; }
    public String getInstanceName() { return instanceName; }
    public long getSshTimeoutMs() { return sshTimeoutMs; }
    public long getConnectRetrySleepMs() { return connectRetrySleepMs; }
    public long getPollIntervalMs() { return pollIntervalMs; }
}
