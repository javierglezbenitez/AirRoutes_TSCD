
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
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
        this.http = HttpClient.newHttpClient();
    }

    public String health() throws IOException, InterruptedException {
        return get("/api/health");
    }

    public String topDegree(int k) throws IOException, InterruptedException {
        return get("/api/graph/top-degree?k=" + k);
    }

    public String shortestPath(String source, String target) throws IOException, InterruptedException {
        String qs = String.format("source=%s&target=%s",
                url(source), url(target));
        return get("/api/graph/shortest-path?" + qs);
    }

    public String isolated() throws IOException, InterruptedException {
        return get("/api/graph/isolated");
    }

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
