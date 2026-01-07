
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

    private final String datamartMode; // "EC2" (por defecto) o "LOCAL"
    private final String neo4jBoltUri; // NEO4J_URI o NEO4J_BOLT_URI

    public AppConfig(Map<String, String> env) {
        this.region = env.getOrDefault("AWS_REGION", "");
        this.bucket = env.getOrDefault("S3_BUCKET", "");
        this.neo4jUser = env.getOrDefault("NEO4J_USER", "");
        this.neo4jPassword = Objects.requireNonNull(env.get("NEO4J_PASSWORD"), "");
        this.instanceName = env.getOrDefault("EC2_INSTANCE_NAME", "");
        this.sshTimeoutMs = Long.parseLong(String.valueOf(180000));
        this.connectRetrySleepMs = Long.parseLong(String.valueOf(10000));
        this.pollIntervalMs = Long.parseLong(String.valueOf(40000));

        // Default conserva comportamiento actual
        this.datamartMode = env.get("DATAMART_MODE");

        // Soporta NEO4J_URI (tu .env) o NEO4J_BOLT_URI
        String uri = env.get("NEO4J_URI");
        this.neo4jBoltUri = uri;
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
