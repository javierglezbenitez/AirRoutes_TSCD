
package infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class NdjsonParser implements RouteParser {
    private final RouteFieldValidator validator = new RouteFieldValidator();
    private final RouteMapper mapperUtil = new RouteMapper();

    @Override
    public List<Map<String, Object>> tryParse(Path file, ObjectMapper mapper) {
        List<Map<String, Object>> routes = new ArrayList<>();
        int addedLines = 0;

        try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    JsonNode node = mapper.readTree(line);
                    if (node.isObject() && validator.hasRouteFields(node)) {
                        routes.add(mapperUtil.map(node));
                        addedLines++;
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignored) {
        }

        if (addedLines > 0) {
            System.out.println("   ✔ Detectado NDJSON. Parseadas " + addedLines + " rutas de " + file.getFileName());
        }
        return routes;
    }
}