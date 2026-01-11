
package repository;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Neo4jContainerSmokeIT {

    @Test
    void canStartNeo4jContainer() {
        Neo4jContainer<?> neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.18"))
                .withEnv("NEO4J_AUTH", "none") // ✅ sin autenticación
                .withEnv("NEO4J_server_memory_heap_initial__size", "256m")
                .withEnv("NEO4J_server_memory_heap_max__size", "512m")
                .withEnv("NEO4J_server_memory_pagecache_size", "128m")
                // Espera explícita hasta que Bolt esté disponible
                .waitingFor(Wait.forLogMessage(".*Bolt enabled on 0\\.0\\.0\\.0:7687.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(5)))
                .withStartupTimeout(Duration.ofMinutes(5));

        try {
            neo4j.start();

            // ⚠️ En tu versión del driver, usa (long, TimeUnit), no Duration
            Config cfg = Config.builder()
                    .withMaxTransactionRetryTime(30, TimeUnit.SECONDS)
                    .build();

            try (Driver driver = GraphDatabase.driver(
                    neo4j.getBoltUrl(),
                    AuthTokens.none(), // ✅ sin credenciales
                    cfg
            );
                 Session session = driver.session()) {

                var rec = session.run("RETURN 1 AS ok").single();
                assertEquals(1, rec.get("ok").asInt());
            }
        } finally {
            neo4j.stop();
        }
    }
}
