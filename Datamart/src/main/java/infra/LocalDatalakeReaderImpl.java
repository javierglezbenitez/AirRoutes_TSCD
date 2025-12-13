
package infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class LocalDatalakeReaderImpl implements DatalakeReader {
    private final Path basePath;

    /**
     * @param baseDir directorio base del datalake local:
     *                - Si apunta a la raíz o a "storage":          <base>/datalake/YYYY-MM-DD/*.json
     *                - Si apunta a "datalake":                      <base>/YYYY-MM-DD/*.json
     *                - Si ya apunta a una carpeta de fecha (YYYY-MM-DD): <base>/*.json
     */
    public LocalDatalakeReaderImpl(String baseDir) {
        this.basePath = Paths.get(baseDir == null || baseDir.isBlank() ? "." : baseDir).toAbsolutePath();
        System.out.println("   📂 LocalDatalake base: " + this.basePath);
    }

    @Override
    public List<String> listFilesForDate(LocalDate date) {
        String dateStr = date.toString(); // YYYY-MM-DD
        Path rootPlusDatalake = basePath.resolve("datalake").resolve(dateStr); // <base>/datalake/YYYY-MM-DD
        Path datalakePlusDate = basePath.resolve(dateStr);                     // <base>/YYYY-MM-DD
        Path baseAsDate       = basePath;                                      // <base> (si ya es carpeta del día)

        Path dir = pickFirstExistingDir(rootPlusDatalake, datalakePlusDate, baseAsDate, dateStr);

        System.out.println("   🔎 Carpeta efectiva LOCAL: " + (dir != null ? dir : "<no encontrada>"));
        List<String> keys = new ArrayList<>();

        if (dir == null) return keys;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : stream) {
                // Clave relativa a basePath para logs consistentes
                keys.add(basePath.relativize(p).toString().replace('\\', '/'));
            }
        } catch (IOException e) {
            System.err.println("   ❌ Error listando LOCAL: " + e.getMessage());
        }
        return keys;
    }

    private Path pickFirstExistingDir(Path rootPlusDatalake, Path datalakePlusDate, Path baseAsDate, String dateStr) {
        if (Files.isDirectory(rootPlusDatalake)) return rootPlusDatalake;
        if (Files.isDirectory(datalakePlusDate)) return datalakePlusDate;
        Path fileName = basePath.getFileName();
        if (fileName != null && fileName.toString().equals(dateStr) && Files.isDirectory(basePath)) {
            return basePath;
        }
        return null;
    }

    @Override
    public List<Map<String, Object>> readSpecificKeys(List<String> keys) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> routes = new ArrayList<>();
        for (String key : keys) {
            Path file = basePath.resolve(key);
            System.out.println("   ⬇️  Leyendo LOCAL: " + file);
            if (!Files.exists(file)) {
                System.out.println("   ⚠️ Archivo no existe: " + file);
                continue;
            }

            // 1) Intento estándar: array o { routes: [...] }
            try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
                JsonNode root = mapper.readTree(is);
                JsonNode arr = root.isArray() ? root : root.path("routes");
                if (arr.isArray()) {
                    int before = routes.size();
                    for (JsonNode node : arr) routes.add(mapRoute(node));
                    int added = routes.size() - before;
                    System.out.println("   ✔ Parseadas " + added + " rutas de " + key);
                    continue;
                }

                // 2) ➕ Soporte extra: objeto único con los campos esperados
                if (root.isObject() && hasRouteFields(root)) {
                    routes.add(mapRoute(root));
                    System.out.println("   ✔ Detectado objeto único. Parseada 1 ruta de " + key);
                    continue;
                }
            }

            // 3) ➕ Soporte extra: NDJSON (una ruta por línea)
            try (InputStream is2 = Files.newInputStream(file, StandardOpenOption.READ);
                 java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is2))) {
                int addedLines = 0;
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    try {
                        JsonNode node = mapper.readTree(line);
                        if (node.isObject() && hasRouteFields(node)) {
                            routes.add(mapRoute(node));
                            addedLines++;
                        }
                    } catch (Exception ignore) { /* línea no JSON válida, se omite */ }
                }
                if (addedLines > 0) {
                    System.out.println("   ✔ Detectado NDJSON. Parseadas " + addedLines + " rutas de " + key);
                    continue;
                }
            }

            System.out.println("   ❌ JSON inválido (no es array, ni 'routes', ni objeto único, ni NDJSON). Omito " + key);
        }
        return routes;
    }

    private boolean hasRouteFields(JsonNode node) {
        return node.has("codigoVuelo") && node.has("origen") && node.has("destino")
                && node.has("duracionMinutos") && node.has("precio")
                && node.has("aerolinea") && node.has("timestamp");
    }

    private Map<String, Object> mapRoute(JsonNode node) {
        Map<String, Object> r = new HashMap<>();
        r.put("codigoVuelo",     node.path("codigoVuelo").asText());
        r.put("origen",          node.path("origen").asText());
        r.put("destino",         node.path("destino").asText());
        r.put("duracionMinutos", node.path("duracionMinutos").asInt());
        r.put("precio",          node.path("precio").asDouble());
        r.put("aerolinea",       node.path("aerolinea").asText());
        r.put("timestamp",       node.path("timestamp").asLong());
        return r;
    }
}
