
package infra;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

interface RouteParser {

    List<Map<String, Object>> tryParse(Path file, ObjectMapper mapper);
}
