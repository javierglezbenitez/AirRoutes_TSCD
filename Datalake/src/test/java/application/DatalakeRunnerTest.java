
package application;

import architecture.AirRoute;
import architecture.Datalake;
import architecture.Storage;
import architecture.RouteGenerator;   // 👈 Import correcto
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import storage.LocalStorage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;     // 👈 Para usar IntStream

import static org.junit.jupiter.api.Assertions.*;

class DatalakeRunnerTest {

    @TempDir
    Path baseDir;

    // Implementa la interfaz del paquete 'architecture'
    static class FixedGenerator implements RouteGenerator {
        private final AtomicInteger seq = new AtomicInteger(1);

        @Override
        public List<AirRoute> generate(int count) {
            return IntStream.range(0, count)
                    .mapToObj(i -> new AirRoute(
                            "FL-" + seq.getAndIncrement(), "MAD", "JFK",
                            100, 100.0, "Iberia", System.currentTimeMillis(), "None", 3))
                    .toList();
        }
    }

    @Test
    void run_shouldWriteBatchesAndStopGracefully() throws Exception {
        Storage storage = new LocalStorage(baseDir.toString());
        Datalake datalake = new Datalake(storage);
        RouteGenerator gen = new FixedGenerator();  // 👈 Ahora coincide el tipo

        int batchSize = 5;
        long interval = 50L;

        DatalakeRunner runner = new DatalakeRunner(gen, datalake, batchSize, interval);
        Thread t = new Thread(runner, "test-runner");
        t.start();

        // Espera a que al menos un ciclo se ejecute
        Thread.sleep(120);
        runner.stop();
        t.join(2000);

        Path folder = baseDir.resolve("datalake").resolve(LocalDate.now().toString());
        assertTrue(Files.exists(folder), "No existe la carpeta del día");

        long files = Files.list(folder).filter(p -> p.toString().endsWith(".json")).count();
        assertTrue(files >= batchSize, "Debe haber al menos un batch escrito");
    }
}
