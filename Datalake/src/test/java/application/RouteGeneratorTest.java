package application;

import architecture.AirRoute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void generate_shouldRespectConstraintsAndLoadParamsFromFile() throws IOException {
        Path params = tempDir.resolve("route_values.txt");
        String content = """
      [AEROLINEAS]
      Iberia
      LATAM
      Air Europa
      [AEROPUERTOS]
      MAD
      JFK
      CDG
      """;
        Files.writeString(params, content);

        RouteGenerator gen = new RouteGenerator(params.toString());
        List<AirRoute> routes = gen.generate(20);

        assertEquals(20, routes.size());

        for (AirRoute ar : routes) {
            assertNotNull(ar.getCodigoVuelo());
            assertNotNull(ar.getOrigen());
            assertNotNull(ar.getDestino());
            assertNotEquals(ar.getOrigen(), ar.getDestino(), "Origen y destino no deben ser iguales");

            if (!"None".equals(ar.getEscala())) {
                assertNotEquals(ar.getEscala(), ar.getOrigen());
                assertNotEquals(ar.getEscala(), ar.getDestino());
            }

            assertTrue(ar.getPrecio() >= 0.0 && ar.getPrecio() <= 500.0, "Precio fuera de rango");
            assertTrue(ar.getDuracionMinutos() >= 30 && ar.getDuracionMinutos() <= 330, "Duración fuera de rango");
            assertTrue(ar.getEmbarque() >= 1 && ar.getEmbarque() <= 7, "Embarque fuera de 1..7");
        }
    }
}