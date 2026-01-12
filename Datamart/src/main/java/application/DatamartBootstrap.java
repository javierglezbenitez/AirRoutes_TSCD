package application;

public class DatamartBootstrap {

    private Thread thread;

    public void runSync() throws Exception {
        AppConfig cfg = new AppConfig(System.getenv());

        String mode = cfg.getDatamartMode();
        switch (mode.toUpperCase()) {
            case "LOCAL":
                System.out.println("🏁 [DATAMART] Iniciando en modo LOCAL (Neo4j local / Docker)...");
                new LocalDatamartOrchestrator(cfg).run();
                break;
            case "EC2":
            default:
                System.out.println("🏁 [DATAMART] Iniciando en modo EC2 (provisioning + SSH + ingesta S3)...");
                new DatamartOrchestrator(cfg).run();
                break;
        }

        System.out.println("✅ [DATAMART] Ejecución finalizada.");
    }

    public void startAsync() {
        if (thread != null && thread.isAlive()) {
            System.out.println("ℹ️ [DATAMART] Ya está en ejecución.");
            return;
        }

        thread = new Thread(() -> {
            try {
                runSync();
            } catch (Exception e) {
                System.err.println("❌ [DATAMART] Error en ejecución: " + e.getMessage());
                e.printStackTrace();
            }
        }, "DatamartOrchestrator");
        thread.start();
    }

    public void stop() {
        if (thread != null && thread.isAlive()) {
            System.out.println("🛑 [DATAMART] Solicitando parada...");
            thread.interrupt();
            try {
                thread.join(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("✅ [DATAMART] Parada completada.");
        }
    }
}