
package repository;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Neo4jContainerSmokeIT {

    @Test
    void canStartNeo4jContainer() {
        Neo4jContainer<?> neo4j = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.18"))
                .withEnv("NEO4J_AUTH", "none") // sin autenticación
                .withEnv("NEO4J_server_memory_heap_initial__size", "256m")
                .withEnv("NEO4J_server_memory_heap_max__size", "512m")
                .withEnv("NEO4J_server_memory_pagecache_size", "128m")
                // Expón puertos explícitos (evita confusiones con efímeros)
                .withExposedPorts(7687, 7474)
                // Espera robusta: HTTP 200 en "/"
                .waitingFor(
                        Wait.forHttp("/")
                                .forPort(7474)
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(6))
                )
                .withStartupTimeout(Duration.ofMinutes(6))
                // Log de contenedor para diagnosticar arranques
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("neo4j-container")));

        try {
            neo4j.start();

            // Construye Bolt URL con el puerto mapeado por Testcontainers
            int boltPort = neo4j.getMappedPort(7687);
            String boltUrl = "bolt://localhost:" + boltPort;

            Config cfg = Config.builder()
                    .withMaxTransactionRetryTime(30, TimeUnit.SECONDS)
                    .build();

            try (Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.none(), cfg);
                 Session session = driver.session()) {

                var rec = session.run("RETURN 1 AS ok").single();
                assertEquals(1, rec.get("ok").asInt());
            }
        } finally {
            neo4j.stop();
        }
    }
}
