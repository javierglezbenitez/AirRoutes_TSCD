
package livedu.api.core;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GraphService {

    private final Driver driver;

    public GraphService(Driver driver) {
        this.driver = driver;
    }

    /**
     * Lista de tickets disponibles entre origen y destino.
     * Incluye aerolínea, precio, duración, embarque y si hay escala (y cuál).
     */
    public List<Map<String, Object>> tickets(String origen, String destino, int limit) {
        String query = """
            MATCH (o:Aeropuerto {codigo:$origen})-[:ORIGEN]->(v:Vuelo)-[:DESTINO]->(d:Aeropuerto {codigo:$destino})
            OPTIONAL MATCH (a:Aerolinea)-[:OPERA]->(v)
            OPTIONAL MATCH (v)-[:ESCALA]->(e:Aeropuerto)
            RETURN v.codigo AS vuelo,
                   a.nombre AS aerolinea,
                   o.codigo AS origen,
                   d.codigo AS destino,
                   coalesce(v.escala, 'None') AS escala,
                   e.codigo AS aeropuertoEscala,
                   v.precio AS precio,
                   coalesce(v.duracion, v.duracionMinutos) AS duracionMin,
                   v.embarque AS embarque,
                   v.timestamp AS timestamp
            ORDER BY precio ASC
            LIMIT $limit
        """;
        return runQuery(query, Map.of("origen", origen, "destino", destino, "limit", limit));
    }

    /**
     * Top N tickets más baratos entre origen y destino.
     */
    public List<Map<String, Object>> ticketsBaratos(String origen, String destino, int limit) {
        String query = """
            MATCH (o:Aeropuerto {codigo:$origen})-[:ORIGEN]->(v:Vuelo)-[:DESTINO]->(d:Aeropuerto {codigo:$destino})
            OPTIONAL MATCH (a:Aerolinea)-[:OPERA]->(v)
            OPTIONAL MATCH (v)-[:ESCALA]->(e:Aeropuerto)
            RETURN v.codigo AS vuelo,
                   a.nombre AS aerolinea,
                   coalesce(v.escala, 'None') AS escala,
                   e.codigo AS aeropuertoEscala,
                   v.precio AS precio,
                   coalesce(v.duracion, v.duracionMinutos) AS duracionMin,
                   v.embarque AS embarque
            ORDER BY precio ASC
            LIMIT $limit
        """;
        return runQuery(query, Map.of("origen", origen, "destino", destino, "limit", limit));
    }

    /**
     * Solo tickets directos (sin escala) entre origen y destino.
     */
    public List<Map<String, Object>> ticketsDirectos(String origen, String destino, int limit) {
        String query = """
            MATCH (o:Aeropuerto {codigo:$origen})-[:ORIGEN]->(v:Vuelo)-[:DESTINO]->(d:Aeropuerto {codigo:$destino})
            WHERE v.escala IS NULL OR v.escala = 'None'
            OPTIONAL MATCH (a:Aerolinea)-[:OPERA]->(v)
            RETURN v.codigo AS vuelo,
                   a.nombre AS aerolinea,
                   v.precio AS precio,
                   coalesce(v.duracion, v.duracionMinutos) AS duracionMin,
                   v.embarque AS embarque
            ORDER BY precio ASC
            LIMIT $limit
        """;
        return runQuery(query, Map.of("origen", origen, "destino", destino, "limit", limit));
    }

    /**
     * Resumen por aerolínea en la ruta: min/avg/max de precio y duración media.
     * (No lleva embarque porque es agregación por aerolínea.)
     */
    public List<Map<String, Object>> resumenRuta(String origen, String destino) {
        String query = """
            MATCH (o:Aeropuerto {codigo:$origen})-[:ORIGEN]->(v:Vuelo)-[:DESTINO]->(d:Aeropuerto {codigo:$destino})
            MATCH (a:Aerolinea)-[:OPERA]->(v)
            RETURN a.nombre AS aerolinea,
                   count(v) AS vuelos,
                   round(min(v.precio)*100)/100 AS minPrecio,
                   round(max(v.precio)*100)/100 AS maxPrecio,
                   round(avg(v.precio)*100)/100 AS avgPrecio,
                   round(avg(coalesce(v.duracion, v.duracionMinutos))) AS avgDuracionMin
            ORDER BY avgPrecio ASC, vuelos DESC
        """;
        return runQuery(query, Map.of("origen", origen, "destino", destino));
    }

    /**
     * Disponibilidad de la ruta: total, directos y con escala.
     */
    public List<Map<String, Object>> disponibilidadRuta(String origen, String destino) {
        String query = """
            MATCH (o:Aeropuerto {codigo:$origen})-[:ORIGEN]->(v:Vuelo)-[:DESTINO]->(d:Aeropuerto {codigo:$destino})
            WITH collect(v) AS vuelos
            RETURN size(vuelos) AS total,
                   size([x IN vuelos WHERE x.escala IS NULL OR x.escala = 'None']) AS directos,
                   size([x IN vuelos WHERE x.escala IS NOT NULL AND x.escala <> 'None']) AS conEscala
        """;
        return runQuery(query, Map.of("origen", origen, "destino", destino));
    }

    // Utilidad común para lanzar lecturas Cypher y mapear a List<Map<String,Object>>
    private List<Map<String, Object>> runQuery(String query, Map<String, Object> params) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> tx.run(query, params)
                    .list(record -> record.asMap()));
        }
    }
}
