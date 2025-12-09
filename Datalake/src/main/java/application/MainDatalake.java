
package application;

import architecture.Datalake;
import architecture.Storage;
import architecture.StorageFactory;

public class MainDatalake {

    public static void main(String[] args) {
        String storageMode = System.getenv().getOrDefault("STORAGE_MODE", "S3");

        StorageFactory factory = new DefaultStorageFactory();
        Storage storage = factory.createStorage(storageMode);
        Datalake datalake = new Datalake(storage);

        RouteGenerator generator = new RouteGenerator();

        int batchSize = Integer.parseInt(System.getenv().getOrDefault("BATCH_SIZE", "30"));
        long intervalMillis = Long.parseLong(System.getenv().getOrDefault("INTERVAL_MS", "30000"));

        DatalakeRunner runner = new DatalakeRunner(generator, datalake, batchSize, intervalMillis);
        Thread t = new Thread(runner, "AirRoutesGenerator");
        t.start();

        System.out.println("✈️ Servicio de AirRoutes iniciado en modo: " + storageMode);

        Runtime.getRuntime().addShutdownHook(new Thread(runner::stop));
    }
}
