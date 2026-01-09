
package infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

public class S3DatalakeReaderImpl implements DatalakeReader {
    private final S3Client s3;
    private final String bucket;

    public S3DatalakeReaderImpl(S3Client s3, String bucket) { this.s3 = s3; this.bucket = bucket; }

    @Override
    public List<String> listFilesForDate(LocalDate date) {
        String prefix = "datalake/" + date + "/";
        System.out.println("   🔎 Prefijo S3: " + prefix);
        List<String> keys = new ArrayList<>();

        String token = null;
        do {
            ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket).prefix(prefix);
            if (token != null && !token.isEmpty()) {
                req.continuationToken(token);
                System.out.println("   ⏭️  ContinuationToken: " + token);
            }
            ListObjectsV2Response resp = s3.listObjectsV2(req.build());
            for (S3Object obj : resp.contents()) {
                if (obj.key().endsWith(".json")) { keys.add(obj.key()); }
            }
            token = resp.nextContinuationToken();
        } while (token != null && !token.isEmpty());

        return keys;
    }

    @Override
    public List<Map<String, Object>> readSpecificKeys(List<String> keys) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> routes = new ArrayList<>();
        for (String key : keys) {
            System.out.println("   ⬇️  Descargando: s3://" + bucket + "/" + key);
            try (InputStream is = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                JsonNode root = mapper.readTree(is);
                JsonNode arr = root.isArray() ? root : root.path("routes");
                if (!arr.isArray()) {
                    System.out.println("   ❌ JSON inválido (no es array ni 'routes'). Omito " + key);
                    continue;
                }
                int before = routes.size();
                for (JsonNode node : arr) { routes.add(mapRoute(node)); }
                int added = routes.size() - before;
                System.out.println("   ✔ Parseadas " + added + " rutas de " + key);
            }
        }
        return routes;
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
        r.put("escala",          node.path("escala").asText());
        r.put("embarque",       node.path("embarque").asInt());
        return r;
    }
}
