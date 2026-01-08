
package livedu.cli;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private final String baseUrl;
    private final HttpClient http;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newHttpClient();
    }

    // Salud
    public String health() throws IOException, InterruptedException {
        return get("/api/health");
    }

    // Tickets: todos
    public String tickets(String origen, String destino, int limit) throws IOException, InterruptedException {
        String qs = String.format("origen=%s&destino=%s&limit=%d", url(origen), url(destino), limit);
        return get("/api/graph/tickets?" + qs);
    }

    // Tickets: baratos (top N)
    public String ticketsBaratos(String origen, String destino, int limit) throws IOException, InterruptedException {
        String qs = String.format("origen=%s&destino=%s&limit=%d", url(origen), url(destino), limit);
        return get("/api/graph/tickets/baratos?" + qs);
    }

    // Tickets: más barato (limit=1)
    public String masBarato(String origen, String destino) throws IOException, InterruptedException {
        String qs = String.format("origen=%s&destino=%s&limit=%d", url(origen), url(destino), 1);
        return get("/api/graph/tickets/baratos?" + qs);
    }

    // Tickets: solo directos
    public String ticketsDirectos(String origen, String destino, int limit) throws IOException, InterruptedException {
        String qs = String.format("origen=%s&destino=%s&limit=%d", url(origen), url(destino), limit);
        return get("/api/graph/tickets/directos?" + qs);
    }

    // Resumen por aerolínea en ruta
    public String resumenRuta(String origen, String destino) throws IOException, InterruptedException {
        String qs = String.format("origen=%s&destino=%s", url(origen), url(destino));
        return get("/api/graph/ruta/resumen?" + qs);
    }

    // Disponibilidad de la ruta (directos vs con escala)
    public String disponibilidadRuta(String origen, String destino) throws IOException, InterruptedException {
        String qs = String.format("origen=%s&destino=%s", url(origen), url(destino));
        return get("/api/graph/ruta/disponibilidad?" + qs);
    }

    // ---- utilidades HTTP ----

    private String get(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 200 && res.statusCode() < 300) {
            return res.body();
        } else {
            return String.format("{\"error\":\"HTTP_%d\",\"body\":%s}", res.statusCode(), quote(res.body()));
        }
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }
}
