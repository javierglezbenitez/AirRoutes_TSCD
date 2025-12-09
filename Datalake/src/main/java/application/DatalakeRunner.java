
package application;

import architecture.Datalake;
import architecture.RouteGenerator;
import architecture.AirRoute;

import java.util.List;

public class DatalakeRunner implements Runnable {

    private final RouteGenerator generator;
    private final Datalake datalake;
    private final int batchSize;
    private final long intervalMillis;
    private volatile boolean running = true;

    public DatalakeRunner(RouteGenerator generator, Datalake datalake,
                          int batchSize, long intervalMillis) {
        this.generator = generator;
        this.datalake = datalake;
        this.batchSize = batchSize;
        this.intervalMillis = intervalMillis;
    }

    @Override
    public void run() {
        while (running) {
            try {
                List<AirRoute> routes = generator.generate(batchSize);
                System.out.println(routes.size());
                datalake.saveAirRoutes(routes);
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                System.out.println("Hilo interrumpido");
                running = false;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("❌ Error en DatalakeRunner: " + e.getMessage());
            }
        }
    }

    public void stop() { running = false; }
}
