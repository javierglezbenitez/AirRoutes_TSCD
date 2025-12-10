
package service;

import repository.GraphRepository;

import java.util.List;
import java.util.Map;

public class DataMartServiceImpl implements DataMartService {
    private final GraphRepository repo;

    public DataMartServiceImpl(GraphRepository repo) { this.repo = repo; }

    @Override
    public void upsertToday(List<Map<String, Object>> routes) {
        System.out.println("   🔄 Actualizando DataMart de HOY en Neo4j...");
        repo.insertAirRouteBatch(routes);
        System.out.println("   ✅ DataMart de HOY actualizado (" + routes.size() + " rutas)");
    }

    @Override
    public void clearOld() {
        System.out.println("   🧽 Limpiando rutas antiguas del DataMart...");
        repo.clearAll();
        System.out.println("   🧹 Rutas antiguas eliminadas de Neo4j");
    }
}
