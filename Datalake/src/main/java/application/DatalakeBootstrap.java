
package application;

import architecture.Datalake;
import architecture.Storage;
import architecture.StorageFactory;

import java.io.IOException;

public class DatalakeBootstrap {
    private Thread thread;
    private DatalakeRunner runner;


    public void start() throws IOException {
        String storageMode = System.getenv().get("DATALAKE_MODE");

        StorageFactory factory = new DefaultStorageFactory();
        Storage storage = factory.createStorage(storageMode);
        Datalake datalake = new Datalake(storage);
        String routePath = System.getenv().get("ROUTE_FILE_PATH");
        RouteGenerator generator = new RouteGenerator(routePath);

        int batchSize = Integer.parseInt(String.valueOf(30));
        long intervalMillis = Long.parseLong(String.valueOf(30000));

        runner = new DatalakeRunner(generator, datalake, batchSize, intervalMillis);
        thread = new Thread(runner, "AirRoutesGenerator");
        thread.start();

        System.out.println("✈️ [DATALAKE] Servicio AirRoutes iniciado (modo: " + storageMode + ")");
    }

    public void stop() {
        if (runner != null) {
            try {
                runner.stop();
            } catch (Exception e) {
                System.err.println("⚠️ [DATALAKE] Error al pedir stop(): " + e.getMessage());
            }
        }
        if (thread != null && thread.isAlive()) {
            try {
                thread.interrupt();       // por si está en Thread.sleep(...)
                thread.join(5_000);       // espera hasta 5s
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("🛑 [DATALAKE] Datalake detenido.");
    }
}