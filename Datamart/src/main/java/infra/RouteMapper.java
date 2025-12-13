
package infra;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

class RouteMapper {
    Map<String, Object> map(JsonNode node) {
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
