
package application;

import java.io.IOException;

public class MainAirRoutes {
    public static void main(String[] args) throws IOException {
        // Instancias de cada módulo
        DatalakeBootstrap datalake = new DatalakeBootstrap();
        DatamartBootstrap datamart = new DatamartBootstrap();

        // 1) Arranca Datalake (no bloquea)
        System.out.println("🚀 Orchestrator: arrancando Datalake...");
        datalake.start();

        // 2) Espera fija de 10s antes de Datamart (puedes parametrizarla si quieres)
        final int delaySec = 10;
        System.out.printf("⏱ Orchestrator: esperando %d segundos antes de Datamart...%n", delaySec);
        try {
            Thread.sleep(delaySec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Orchestrator: espera interrumpida");
        }

        // 3) Lanza Datamart (elige SYNC vs ASYNC)
        boolean asyncDatamart = false; // pon a true si quieres que ambos corran en paralelo
        if (asyncDatamart) {
            System.out.println("🚀 Orchestrator: lanzando Datamart en ASYNC...");
            datamart.startAsync();
        } else {
            System.out.println("🚀 Orchestrator: lanzando Datamart en SYNC...");
            try {
                datamart.runSync(); // bloquea hasta que termine
            } catch (Exception e) {
                System.err.println("❌ Orchestrator: error ejecutando Datamart: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 4) Shutdown hook: asegura parada ordenada
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🧹 Orchestrator: realizando shutdown");
            try {
                datamart.stop(); // solo tiene efecto si estaba en async
            } catch (Exception ignored) {}
            try {
                datalake.stop(); // detiene el runner del Datalake
            } catch (Exception ignored) {}
            System.out.println("✅ Orchestrator: shutdown completo.");
        }));
    }
}