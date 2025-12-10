
package application.workflow;

import infra.DatalakeReader;
import service.DataMartService;

import java.time.LocalDate;
import java.util.*;

public final class S3IngestionWorkflow {
    private S3IngestionWorkflow() {}

    public static void runForever(DatalakeReader reader, DataMartService service, long pollMs) throws InterruptedException {
        Set<String> processedKeys = new HashSet<>();
        LocalDate currentDate = LocalDate.now();
        System.out.println("📡 Ingesta continua. Día actual: " + currentDate);

        while (true) {
            try {
                LocalDate now = LocalDate.now();
                if (!now.equals(currentDate)) {
                    System.out.println("🔄 Cambio de día (" + currentDate + " -> " + now + "). Reseteando estado...");
                    currentDate = now;
                    processedKeys.clear();
                }
                List<String> keys = reader.listFilesForDate(currentDate);
                List<String> newKeys = keys.stream().filter(k -> !processedKeys.contains(k)).toList();
                if (newKeys.isEmpty()) {
                    System.out.println("😴 No hay nuevos archivos. Esperando " + pollMs / 1000 + "s...");
                } else {
                    System.out.println("➕ Nuevos archivos: " + newKeys.size());
                    List<Map<String, Object>> routes = reader.readSpecificKeys(newKeys);
                    service.upsertToday(routes);
                    processedKeys.addAll(newKeys);
                    System.out.println("✔ Marcadas como procesadas (" + processedKeys.size() + " total)");
                }
                Thread.sleep(pollMs);
            } catch (Exception e) {
                System.err.println("❌ Error en ciclo de ingesta: " + e.getMessage());
                Thread.sleep(Math.max(20_000, pollMs / 2));
            }
        }
    }
}
