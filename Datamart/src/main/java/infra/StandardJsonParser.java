
package infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class StandardJsonParser implements RouteParser {
    private final RouteFieldValidator validator = new RouteFieldValidator();
    private final RouteMapper mapperUtil = new RouteMapper();

    @Override
    public List<Map<String, Object>> tryParse(Path file, ObjectMapper mapper) {
        List<Map<String, Object>> routes = new ArrayList<>();

        try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
            JsonNode root = mapper.readTree(is);

            // Caso 1: array de rutas o { routes: [...] }
            JsonNode arr = root.isArray() ? root : root.path("routes");
            if (arr.isArray()) {
                int before = routes.size();
                for (JsonNode node : arr) {
                    routes.add(mapperUtil.map(node));
                }
                int added = routes.size() - before;
                System.out.println("   ✔ Parseadas " + added + " rutas de " + file.getFileName());
                return routes;
            }

            // Caso 2: objeto único
            if (root.isObject() && validator.hasRouteFields(root)) {
                routes.add(mapperUtil.map(root));
                System.out.println("   ✔ Detectado objeto único. Parseada 1 ruta de " + file.getFileName());
                return routes;
            }
        } catch (Exception ignored) {
            // No imprimimos aquí; el control de logs de error se deja al lector compuesto
        }
        return routes; // vacío si no parseó nada
    }
}