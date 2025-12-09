
package service;

import repository.GraphBuilder;

import java.util.List;
import java.util.Map;

public class DataMartUpdater {
    private final GraphBuilder graphBuilder;

    public DataMartUpdater(GraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    public void updateToday(List<Map<String, Object>> routes) {
        System.out.println("   🔄 Actualizando DataMart de HOY en Neo4j...");
        graphBuilder.insertAirRouteBatch(routes);
        System.out.println("   ✅ DataMart de HOY actualizado (" + routes.size() + " rutas)");
    }

    public void clearOldRoutes() {
        System.out.println("   🧽 Limpiando rutas antiguas del DataMart...");
        graphBuilder.clearAirRoutes();
        System.out.println("   🧹 Rutas antiguas eliminadas de Neo4j");
    }
}
