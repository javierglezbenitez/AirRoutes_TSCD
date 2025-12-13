
package infra;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;

public class LocalDatalakeReaderImpl implements DatalakeReader {
    private final Path basePath;
    private final DirectoryResolver directoryResolver = new DirectoryResolver();
    private final FileKeyLister fileKeyLister = new FileKeyLister();
    private final CompositeRouteParser parser = new CompositeRouteParser();

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
        Path dir = directoryResolver.resolve(basePath, dateStr);

        System.out.println("   🔎 Carpeta efectiva LOCAL: " + (dir != null ? dir : "<no encontrada>"));
        return fileKeyLister.listJsonKeys(dir, basePath);
    }

    @Override
    public List<Map<String, Object>> readSpecificKeys(List<String> keys) throws Exception {
        List<Map<String, Object>> routes = new ArrayList<>();

        for (String key : keys) {
            Path file = basePath.resolve(key);
            System.out.println("   ⬇️  Leyendo LOCAL: " + file);

            if (!Files.exists(file)) {
                System.out.println("   ⚠️ Archivo no existe: " + file);
                continue;
            }

            List<Map<String, Object>> parsed = parser.parse(file);
            if (parsed.isEmpty()) {
                System.out.println("   ❌ JSON inválido (no es array, ni 'routes', ni objeto único, ni NDJSON). Omito " + key);
                continue;
            }
            routes.addAll(parsed);
        }

        return routes;
    }
}