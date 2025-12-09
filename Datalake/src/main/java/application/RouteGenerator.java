
package application;

import architecture.AirRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouteGenerator implements architecture.RouteGenerator {

    private final Random rand = new Random();
    private final String[] aerolineas = {"Iberia", "Vueling", "Ryanair", "Air Europa", "Lufthansa"};
    private final String[] aeropuertos = {"MAD", "BCN", "LPA", "TFN", "BIO", "AGP"};

    @Override
    public List<AirRoute> generate(int count) {
        List<AirRoute> vuelos = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String origen = aeropuertos[rand.nextInt(aeropuertos.length)];
            String destino = aeropuertos[rand.nextInt(aeropuertos.length)];
            while (destino.equals(origen)) {
                destino = aeropuertos[rand.nextInt(aeropuertos.length)];
            }

            vuelos.add(new AirRoute(
                    "FL-" + System.nanoTime(),              // misma lógica para evitar duplicados
                    origen,
                    destino,
                    30 + rand.nextInt(300),
                    Math.round(rand.nextDouble() * 50000.0) / 100.0,
                    aerolineas[rand.nextInt(aerolineas.length)],
                    System.currentTimeMillis()
            ));
        }
        return vuelos;
    }
}
