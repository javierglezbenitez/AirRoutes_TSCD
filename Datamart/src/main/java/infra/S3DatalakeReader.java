
package infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

public class S3DatalakeReader {
    private final S3Client s3;
    private final String bucket;

    public S3DatalakeReader(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public List<String> listFilesForDate(LocalDate date) {
        String prefix = "datalake/" + date + "/";
        System.out.println("   🔎 Prefijo S3: " + prefix);
        List<String> keys = new ArrayList<>();

        String token = null;
        do {
            ListObjectsV2Request.Builder req = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix);
            if (token != null && !token.isEmpty()) {
                req.continuationToken(token);
                System.out.println("   ⏭️  ContinuationToken: " + token);
            }
            ListObjectsV2Response resp = s3.listObjectsV2(req.build());

            for (S3Object obj : resp.contents()) {
                if (obj.key().endsWith(".json")) {
                    keys.add(obj.key());
                }
            }
            token = resp.nextContinuationToken();
        } while (token != null && !token.isEmpty());

        return keys;
    }

    public List<Map<String, Object>> readFilesForDate(LocalDate date) throws Exception {
        List<String> keys = listFilesForDate(date);
        return readSpecificKeys(keys);
    }

    public List<Map<String, Object>> readSpecificKeys(List<String> keys) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> routes = new ArrayList<>();

        for (String key : keys) {
            System.out.println("   ⬇️  Descargando: s3://" + bucket + "/" + key);
            try (InputStream is = s3.getObject(GetObjectRequest.builder()
                    .bucket(bucket).key(key).build())) {

                JsonNode root = mapper.readTree(is);

                if (!root.isArray()) {
                    System.out.println("   ⚠️  El JSON no es array. Intentaré leer un campo 'routes'...");
                }

                JsonNode arr = root.isArray() ? root : root.path("routes");
                if (!arr.isArray()) {
                    System.out.println("   ❌ No es posible iterar: ni array ni 'routes' array. Omito " + key);
                    continue;
                }

                int countBefore = routes.size();
                for (JsonNode node : arr) {
                    routes.add(mapRoute(node));
                }
                int added = routes.size() - countBefore;
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
        return r;
    }
}
