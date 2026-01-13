
package repository;

import org.junit.jupiter.api.*;
import org.neo4j.driver.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class Neo4jGraphRepositoryIT {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.18")
            .withEnv("NEO4J_AUTH", "neo4j/testpassword") // contraseña válida
            .withEnv("NEO4J_server_memory_heap_initial__size", "256m")
            .withEnv("NEO4J_server_memory_heap_max__size", "512m")
            .withEnv("NEO4J_server_memory_pagecache_size", "128m")
            .withExposedPorts(7687, 7474)
            .waitingFor(
                    Wait.forHttp("/")
                            .forPort(7474)
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(6))
            )
            .withStartupTimeout(Duration.ofMinutes(6))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("neo4j-container")));

    static Driver driver;
    static Neo4jGraphRepository repo;

    @BeforeAll
    static void setup() {
        int boltPort = neo4j.getMappedPort(7687);
        String boltUrl = "bolt://localhost:" + boltPort;

        Config cfg = Config.builder()
                .withMaxTransactionRetryTime(30, TimeUnit.SECONDS)
                .build();

        driver = GraphDatabase.driver(
                boltUrl,
                AuthTokens.basic("neo4j", "testpassword"),
                cfg
        );
        repo = new Neo4jGraphRepository(driver);
        repo.ensureSchema();
    }

    @AfterAll
    static void tearDown() {
        driver.close();
    }

    @BeforeEach
    void clear() {
        repo.clearAll();
    }

    private static Map<String, Object> route(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void insertAirRouteBatch_shouldCreateNodesRelationships_andHandleEscala() {
        long now = Instant.now().toEpochMilli();

        Map<String, Object> r1 = route(
                "codigoVuelo","FL-1","origen","MAD","destino","JFK",
                "duracionMinutos", 100, "precio", 100.0, "aerolinea","Iberia",
                "timestamp", now, "escala","None", "embarque", 3
        );
        Map<String, Object> r2 = route(
                "codigoVuelo","FL-2","origen","MAD","destino","JFK",
                "duracionMinutos", 120, "precio", 150.0, "aerolinea","LATAM",
                "timestamp", now, "escala","CDG", "embarque", 5
        );

        List<Map<String, Object>> batch = List.<Map<String, Object>>of(r1, r2);
        repo.insertAirRouteBatch(batch);

        try (Session s = driver.session()) {
            int cntFlights = s.run("MATCH (v:Vuelo) RETURN count(v) AS c").single().get("c").asInt();
            assertEquals(2, cntFlights);

            int rutaCount = s.run(
                    "MATCH (:Aeropuerto {codigo:'MAD'})-[r:RUTA]->(:Aeropuerto {codigo:'JFK'}) RETURN count(r) AS c"
            ).single().get("c").asInt();
            assertEquals(1, rutaCount, "RUTA debería ser única por MERGE");

            int escalaForFL2 = s.run(
                    "MATCH (:Vuelo {codigo:'FL-2'})-[:ESCALA]->(e:Aeropuerto {codigo:'CDG'}) RETURN count(e) AS c"
            ).single().get("c").asInt();
            assertEquals(1, escalaForFL2);

            int escalaForFL1 = s.run(
                    "MATCH (:Vuelo {codigo:'FL-1'})-[:ESCALA]->() RETURN count(*) AS c"
            ).single().get("c").asInt();
            assertEquals(0, escalaForFL1);
        }
    }

    @Test
    void insertAirRouteBatch_shouldUpsert_onMatchUpdatesPriceAndDuration() {
        long now = Instant.now().toEpochMilli();

        Map<String, Object> base = route(
                "codigoVuelo","FL-9","origen","CDG","destino","MAD",
                "duracionMinutos",90,"precio",80.0,"aerolinea","Air Europa",
                "timestamp",now,"escala","None","embarque",4
        );
        repo.insertAirRouteBatch(List.<Map<String, Object>>of(base));

        Map<String, Object> updated = route(
                "codigoVuelo","FL-9","origen","CDG","destino","MAD",
                "duracionMinutos",95,"precio",120.0,"aerolinea","Air Europa",
                "timestamp",now + 1000,"escala","None","embarque",4
        );
        repo.insertAirRouteBatch(List.<Map<String, Object>>of(updated));

        try (Session s = driver.session()) {
            var rec = s.run("MATCH (v:Vuelo {codigo:'FL-9'}) RETURN v.precio AS p, v.duracion AS d").single();
            assertEquals(120.0, rec.get("p").asDouble(), 0.001);
            assertEquals(95, rec.get("d").asInt());
        }
    }

    @Test
    void clearAll_shouldRemoveEverything() {
        Map<String, Object> x = route(
                "codigoVuelo","X","origen","A","destino","B",
                "duracionMinutos",10,"precio",1.0,"aerolinea","Z","timestamp",1L,"escala","None","embarque",1
        );

        repo.insertAirRouteBatch(List.<Map<String, Object>>of(x));
        repo.clearAll();

        try (Session s = driver.session()) {
            int nodes = s.run("MATCH (n) RETURN count(n) AS c").single().get("c").asInt();
            assertEquals(0, nodes);
        }
    }
}
