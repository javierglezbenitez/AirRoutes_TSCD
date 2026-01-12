package infra;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalDatalakeReaderImplTest {

    @TempDir
    Path base;

    @Test
    void listAndRead_shouldResolveDateFolder_andParseVariousJsonFormats() throws Exception {
        String today = LocalDate.now().toString();
        Path dayDir = base.resolve("datalake").resolve(today);
        Files.createDirectories(dayDir);

        // 1) Array JSON
        String arrJson = """
      [
        {"codigoVuelo":"FL-1","origen":"MAD","destino":"JFK","duracionMinutos":100,"precio":100.0,"aerolinea":"Iberia","timestamp":1,"escala":"None","embarque":3},
        {"codigoVuelo":"FL-2","origen":"CDG","destino":"MAD","duracionMinutos":90,"precio":50.0,"aerolinea":"LATAM","timestamp":2,"escala":"JFK","embarque":2}
      ]""";
        Files.writeString(dayDir.resolve("arr.json"), arrJson);

        // 2) { routes: [...] }
        String routesJson = """
      {"routes":[
        {"codigoVuelo":"FL-3","origen":"BCN","destino":"LHR","duracionMinutos":120,"precio":70.0,"aerolinea":"Vueling","timestamp":3,"escala":"None","embarque":4}
      ]}
      """;
        Files.writeString(dayDir.resolve("routes.json"), routesJson);

        // 3) Objeto único
        String singleJson = """
      {"codigoVuelo":"FL-4","origen":"LIS","destino":"FRA","duracionMinutos":110,"precio":80.0,"aerolinea":"TAP","timestamp":4,"escala":"None","embarque":5}
      """;
        Files.writeString(dayDir.resolve("single.json"), singleJson);

        // 4) NDJSON (líneas JSON válidas). Lo llamamos .json para que el FileKeyLister lo incluya.
        String ndjson = """
      {"codigoVuelo":"FL-5","origen":"AGP","destino":"AMS","duracionMinutos":130,"precio":90.0,"aerolinea":"KLM","timestamp":5,"escala":"None","embarque":6}
      {"codigoVuelo":"FL-6","origen":"AMS","destino":"AGP","duracionMinutos":125,"precio":95.0,"aerolinea":"KLM","timestamp":6,"escala":"None","embarque":7}
      """;
        Files.writeString(dayDir.resolve("ndjson.json"), ndjson);

        LocalDatalakeReaderImpl reader = new LocalDatalakeReaderImpl(base.toString());

        List<String> keys = reader.listFilesForDate(LocalDate.now());
        assertEquals(4, keys.size());
        assertTrue(keys.stream().allMatch(k -> k.startsWith("datalake/" + today + "/")));

        List<Map<String,Object>> parsed = reader.readSpecificKeys(keys);
        assertEquals(2 + 1 + 1 + 1, parsed.size()); // arr(2) + routes(1) + single(1) + ndjson(1)

        Map<String,Object> first = parsed.get(0);
        assertTrue(first.containsKey("codigoVuelo"));
        assertTrue(first.containsKey("embarque"));
    }
}