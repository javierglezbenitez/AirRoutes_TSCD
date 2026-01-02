
package livedu.api.core;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value; // IMPORT correcto del annotation
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GraphService {

    private final Driver driver;

    @Value("${graph.label:Aeropuerto}")
    private String nodeLabel;

    @Value("${graph.relType:RUTA}")
    private String relType;

    @Value("${graph.visibleProp:codigo}")
    private String visibleProp;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    // Fallback defensivo por si por alguna razón la inyección queda en blanco
    private void ensureConfigDefaults() {
        if (nodeLabel == null || nodeLabel.isBlank()) nodeLabel = "Aeropuerto";
        if (relType == null || relType.isBlank()) relType = "RUTA";
        if (visibleProp == null || visibleProp.isBlank()) visibleProp = "codigo";
    }

    public List<String> shortestPath(String from, String to) {
        ensureConfigDefaults();
        String cypher = String.format("""
                    MATCH (a:%s {%s:$from}), (b:%s {%s:$to})
                    MATCH p = shortestPath((a)-[:%s*..100]-(b))
                    RETURN [n IN nodes(p) | n.%s] AS names
                """, nodeLabel, visibleProp, nodeLabel, visibleProp, relType, visibleProp);

        try (var session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            var records = session.readTransaction(tx ->
                    tx.run(cypher, Map.of("from", from, "to", to))
                            .list(r -> r.get("names").asList(v -> v.asString()))
            );
            return records.isEmpty() ? List.of() : records.get(0);
        }
    }

    public List<String> isolatedNodes() {
        ensureConfigDefaults();
        // Neo4j 5: usa EXISTS para comprobar patrones; COUNT en WHERE no vale
        String cypher = String.format("""
                    MATCH (n:%s)
                    WHERE NOT EXISTS { MATCH (n)--() }
                    RETURN coalesce(n.%s,'') AS name
                    LIMIT 1000
                """, nodeLabel, visibleProp);

        try (var session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            return session.readTransaction(tx ->
                    tx.run(cypher).list(r -> r.get("name").isNull() ? "" : r.get("name").asString())
            );
        }
    }

    public List<Map<String, Object>> topDegree(int k) {
        ensureConfigDefaults();
        // Opción 1: grado contando TODAS las relaciones del nodo
        String cypher = String.format("""
                    MATCH (n:%s)
                    RETURN coalesce(n.%s,'') AS name, COUNT { (n)--() } AS deg
                    ORDER BY deg DESC
                    LIMIT $k
                """, nodeLabel, visibleProp);

        try (var session = driver.session(SessionConfig.forDatabase("neo4j"))) {
            return session.readTransaction(tx ->
                    tx.run(cypher, Map.of("k", k)).list(r -> Map.of(
                            "name", r.get("name").isNull() ? "" : r.get("name").asString(),
                            "degree", r.get("deg").asInt(0)
                    ))
            );
        }
    }
}