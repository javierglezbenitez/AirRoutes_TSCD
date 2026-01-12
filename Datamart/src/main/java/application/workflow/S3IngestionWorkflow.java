
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
        boolean dayCleared = false;
        System.out.println("📡 Ingesta continua. Día actual: " + currentDate);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                LocalDate now = LocalDate.now();

                if (!now.equals(currentDate)) {
                    System.out.println("🔄 Cambio de día (" + currentDate + " -> " + now + "). Reseteando estado de archivos...");
                    currentDate = now;
                    processedKeys.clear();
                    dayCleared = false;
                }

                List<String> keys = reader.listFilesForDate(currentDate);
                List<String> newKeys = keys.stream().filter(k -> !processedKeys.contains(k)).toList();

                if (newKeys.isEmpty()) {
                    System.out.println("😴 No hay nuevos archivos. Esperando " + pollMs / 1000 + "s...");
                    Thread.sleep(pollMs);
                    continue;
                }

                System.out.println("➕ Nuevos archivos: " + newKeys.size());
                List<Map<String, Object>> routes = reader.readSpecificKeys(newKeys);

                if (routes == null || routes.isEmpty()) {
                    System.out.println("⚠️ Archivos nuevos sin rutas válidas. Esperando " + pollMs / 1000 + "s...");
                    Thread.sleep(pollMs);
                    continue;
                }

                if (!dayCleared) {
                    System.out.println("🧽 Primer lote del día " + currentDate + ": limpiando DataMart antes de insertar...");
                    service.clearOld();  // borra TODO el grafo
                    dayCleared = true;
                }

                service.upsertToday(routes);
                processedKeys.addAll(newKeys);
                System.out.println("✔ Marcadas como procesadas (" + processedKeys.size() + " total)");

                Thread.sleep(pollMs);
            } catch (Exception e) {
                System.err.println("❌ Error en ciclo de ingesta: " + e.getMessage());
                Thread.sleep(Math.max(20_000, pollMs / 2));
            }
        }
    }
}
