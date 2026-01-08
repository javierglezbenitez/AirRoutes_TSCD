
package application;

import architecture.AirRoute;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouteGenerator implements architecture.RouteGenerator {

    private final Random rand = new Random();
    private final List<String> aerolineas = new ArrayList<>();
    private final List<String> aeropuertos = new ArrayList<>();

    public RouteGenerator(String filePath) throws IOException {
        cargarParametros(filePath);
    }

    private void cargarParametros(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean leyendoAerolineas = false;
            boolean leyendoAeropuertos = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("[AEROLINEAS]")) {
                    leyendoAerolineas = true;
                    leyendoAeropuertos = false;
                    continue;
                }
                if (line.equalsIgnoreCase("[AEROPUERTOS]")) {
                    leyendoAerolineas = false;
                    leyendoAeropuertos = true;
                    continue;
                }

                if (leyendoAerolineas) {
                    aerolineas.add(line);
                } else if (leyendoAeropuertos) {
                    aeropuertos.add(line);
                }
            }
        }
    }

    @Override
    public List<AirRoute> generate(int count) {
        List<AirRoute> vuelos = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String origen = aeropuertos.get(rand.nextInt(aeropuertos.size()));
            String destino = aeropuertos.get(rand.nextInt(aeropuertos.size()));
            while (destino.equals(origen)) {
                destino = aeropuertos.get(rand.nextInt(aeropuertos.size()));
            }

            // Escala aleatoria
            String escala = "None";
            if (rand.nextBoolean()) {
                escala = aeropuertos.get(rand.nextInt(aeropuertos.size()));
                while (escala.equals(origen) || escala.equals(destino)) {
                    escala = aeropuertos.get(rand.nextInt(aeropuertos.size()));
                }
            }

            vuelos.add(new AirRoute(
                    "FL-" + System.nanoTime(),
                    origen,
                    destino,
                    30 + rand.nextInt(300),
                    Math.round(rand.nextDouble() * 50000.0) / 100.0,
                    aerolineas.get(rand.nextInt(aerolineas.size())),
                    System.currentTimeMillis(),
                    escala
            ));
        }
        return vuelos;
    }
}
