
package infra;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

interface RouteParser {
    /**
     * Intenta parsear el archivo y devolver rutas.
     * Si no puede, devuelve una lista vacía (no debe lanzar).
     */
    List<Map<String, Object>> tryParse(Path file, ObjectMapper mapper);
}
