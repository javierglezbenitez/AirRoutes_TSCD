
package infra;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class CompositeRouteParser {
    private final List<RouteParser> chain;
    private final ObjectMapper mapper;

    CompositeRouteParser() {
        this.mapper = new ObjectMapper();
        this.chain = Arrays.asList(
                new StandardJsonParser(), // primero: array/routes/objeto único
                new NdjsonParser()        // luego: NDJSON
        );
    }

    List<Map<String, Object>> parse(Path file) {
        for (RouteParser parser : chain) {
            List<Map<String, Object>> parsed = parser.tryParse(file, mapper);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return Collections.emptyList();
    }
}

