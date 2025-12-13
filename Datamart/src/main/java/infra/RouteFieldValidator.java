
package infra;

import com.fasterxml.jackson.databind.JsonNode;

class RouteFieldValidator {
    boolean hasRouteFields(JsonNode node) {
        return node.has("codigoVuelo") && node.has("origen") && node.has("destino")
                && node.has("duracionMinutos") && node.has("precio")
                && node.has("aerolinea") && node.has("timestamp");
    }
}
