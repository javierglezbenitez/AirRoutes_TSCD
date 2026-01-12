
package livedu.api.config;

import livedu.api.aws.Ec2TagLocator;
import org.neo4j.driver.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class Neo4jConfig {

    @Value("${neo4j.uri:}")            private String propUri;
    @Value("${neo4j.user:neo4j}")      private String user;
    @Value("${neo4j.pass:neo4j}")      private String pass;

    @Value("${aws.autodiscovery.enabled:true}")
    private boolean awsAuto;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.datamartTag:neo4j-datamart}")
    private String datamartTag;

    @Bean
    @Lazy
    public Driver neo4jDriver() {
        // 1) Primero: NEO4J_URI (env o application.yml)
        String effectiveUri = (propUri != null && !propUri.isBlank())
                ? propUri
                : System.getenv("NEO4J_URI");

        // 2) Si no vino explícita y el auto-discovery está habilitado, resuelve por tag
        if ((effectiveUri == null || effectiveUri.isBlank()) && awsAuto) {
            String ip = Ec2TagLocator.findPublicIpByNameTag(datamartTag, awsRegion);
            if (ip != null && !ip.isBlank()) {
                effectiveUri = "bolt://" + ip + ":7687";
                System.out.println("▶ Neo4j URI (auto): " + effectiveUri + " [tag=" + datamartTag + "]");
            }
        }

        // 3) Fallback final para entornos de desarrollo
        if (effectiveUri == null || effectiveUri.isBlank()) {
            effectiveUri = "bolt://localhost:7687";
            System.out.println("⚠️ Neo4j URI no resuelta; usando fallback " + effectiveUri);
        }

        // Config del driver: timeout corto y pool razonable
        Config cfg = Config.builder()
                .withConnectionTimeout(5, TimeUnit.SECONDS)
                .withMaxConnectionPoolSize(20)
                .withMaxTransactionRetryTime(5,TimeUnit.SECONDS)
                .build();

        // Importante: no ejecutar query aquí; el HealthController valida conexión
        return GraphDatabase.driver(effectiveUri, AuthTokens.basic(user, pass), cfg);
    }
}
