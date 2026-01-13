
package application;

import java.io.IOException;

public class MainAirRoutes {
    public static void main(String[] args) throws IOException {
        DatalakeBootstrap datalake = new DatalakeBootstrap();
        DatamartBootstrap datamart = new DatamartBootstrap();

        System.out.println("🚀 Orchestrator: arrancando Datalake...");
        datalake.start();

        final int delaySec = 10;
        System.out.printf("⏱ Orchestrator: esperando %d segundos antes de Datamart...%n", delaySec);
        try {
            Thread.sleep(delaySec * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("⚠️ Orchestrator: espera interrumpida");
        }

        boolean asyncDatamart = false;
        if (asyncDatamart) {
            System.out.println("🚀 Orchestrator: lanzando Datamart en ASYNC...");
            datamart.startAsync();
        } else {
            System.out.println("🚀 Orchestrator: lanzando Datamart en SYNC...");
            try {
                datamart.runSync();
            } catch (Exception e) {
                System.err.println("❌ Orchestrator: error ejecutando Datamart: " + e.getMessage());
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🧹 Orchestrator: realizando shutdown");
            try {
                datamart.stop();
            } catch (Exception ignored) {}
            try {
                datalake.stop();
            } catch (Exception ignored) {}
            System.out.println("✅ Orchestrator: shutdown completo.");
        }));
    }
}