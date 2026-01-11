
package storage;

import architecture.AirRoute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageTest {

    @TempDir
    Path baseDir;

    @Test
    void saveAirRoutes_shouldWriteOneJsonPerRouteInDateFolder() throws Exception {
        LocalStorage storage = new LocalStorage(baseDir.toString());

        List<AirRoute> routes = List.of(
                new AirRoute("FL-1","MAD","JFK",120,450.5,"Iberia",System.currentTimeMillis(),"None",3),
                new AirRoute("FL-2","CDG","MAD",95,120.0,"LATAM",System.currentTimeMillis(),"JFK",5)
        );

        storage.saveAirRoutes(routes);

        Path folder = baseDir.resolve("datalake").resolve(LocalDate.now().toString());
        assertTrue(Files.exists(folder), "No se creó la carpeta del día");

        long files = Files.list(folder)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .count();

        assertEquals(routes.size(), files, "Debe haber un archivo por ruta");
    }
}
