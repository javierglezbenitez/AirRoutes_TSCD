
package repository;

import org.neo4j.driver.*;

import java.util.List;
import java.util.Map;

public class Neo4jGraphRepository implements GraphRepository {
    private final Driver driver;

    public Neo4jGraphRepository(Driver driver) { this.driver = driver; }

    @Override
    public void insertAirRouteBatch(List<Map<String, Object>> routes) {
        if (routes == null || routes.isEmpty()) {
            System.out.println("   ⚠️ No hay rutas para insertar.");
            return;
        }
        System.out.println("   ✍️ Insertando " + routes.size() + " rutas en Neo4j...");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(
                        "UNWIND $routes AS row " +
                                "MERGE (a:Aerolinea {nombre: row.aerolinea}) " +
                                "MERGE (o:Aeropuerto {codigo: row.origen}) " +
                                "MERGE (d:Aeropuerto {codigo: row.destino}) " +
                                "MERGE (v:Vuelo {codigo: row.codigoVuelo}) " +
                                "ON CREATE SET v.precio = row.precio, " +
                                "              v.duracion = row.duracionMinutos, " +
                                "              v.timestamp = row.timestamp, " +
                                "              v.createdAt = datetime() " +
                                "ON MATCH  SET v.precio = row.precio, " +
                                "              v.duracion = row.duracionMinutos, " +
                                "              v.timestamp = row.timestamp, " +
                                "              v.updatedAt = datetime() " +
                                "MERGE (a)-[:OPERA]->(v) " +
                                "MERGE (o)-[:ORIGEN]->(v) " +
                                "MERGE (v)-[:DESTINO]->(d) " +
                                // ⬇️ NUEVO: relación directa entre aeropuertos
                                "MERGE (o)-[ruta:RUTA]->(d) " +
                                "ON CREATE SET ruta.createdAt = datetime(), " +
                                "              ruta.lastPrice = row.precio, " +
                                "              ruta.lastDuration = row.duracionMinutos, " +
                                "              ruta.lastSeenAt = row.timestamp " +
                                "ON MATCH  SET ruta.updatedAt = datetime(), " +
                                "              ruta.lastPrice = row.precio, " +
                                "              ruta.lastDuration = row.duracionMinutos, " +
                                "              ruta.lastSeenAt = row.timestamp ",
                        Map.of("routes", routes)
                );
                return null;
            });
            System.out.println("   ✔ Inserción completada");
        } catch (Exception e) {
            System.out.println("   ❌ Error insertando rutas: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void clearAll() {
        System.out.println("   🧹 Borrando todo el grafo (Datamart)...");
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> { tx.run("MATCH (n) DETACH DELETE n"); return null; });
            System.out.println("   ✔ Borrado completado");
        }
    }

    @Override
    public void ensureSchema() {
        System.out.println("   🗂️  Creando constraints e índices si no existen...");
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run("CREATE CONSTRAINT v_codigo_unique IF NOT EXISTS FOR (v:Vuelo) REQUIRE v.codigo IS UNIQUE");
                tx.run("CREATE INDEX a_nombre_idx IF NOT EXISTS FOR (a:Aerolinea) ON (a.nombre)");
                tx.run("CREATE INDEX ap_codigo_idx IF NOT EXISTS FOR (ap:Aeropuerto) ON (ap.codigo)");
                // (Opcional, Neo4j 5): índice sobre propiedades de relaciones RUTA
                // tx.run("CREATE INDEX ruta_lastSeen_idx IF NOT EXISTS FOR ()-[r:RUTA]-() ON (r.lastSeenAt)");
                return null;
            });
            System.out.println("   ✔ Constraints/índices OK");
        } catch (Exception e) {
            System.err.println("   ❌ Error en ensureSchema: " + e.getMessage());
        }
    }
}
