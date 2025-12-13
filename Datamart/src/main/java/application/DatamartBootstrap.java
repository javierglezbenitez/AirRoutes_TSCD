
package application;

/**
 * Bootstrap del módulo Datamart.
 * Expone runSync()/startAsync()/stop() para controlarlo desde el Orchestrator.
 */
public class DatamartBootstrap {

    private Thread thread;

    /**
     * Ejecuta el Datamart de forma SÍNCRONA (bloquea hasta terminar).
     * Reutiliza la lógica existente sin tocar nada.
     */
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

    /**
     * Arranca el Datamart en un hilo propio (no bloquea).
     * Ideal si quieres que Datalake y Datamart corran en paralelo.
     */
    public void startAsync() {
        // Evita arrancar dos veces
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

    /**
     * Solicita la parada ordenada si se lanzó en async.
     * Como la lógica de ingesta es "runForever(...)",
     * usamos interrupción del hilo para que salga del sleep y termine.
     */
    public void stop() {
        if (thread != null && thread.isAlive()) {
            System.out.println("🛑 [DATAMART] Solicitando parada...");
            thread.interrupt(); // Las ingestas suelen dormir entre polls; esto las despierta
            try {
                thread.join(5_000); // espera hasta 5s a que cierre
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("✅ [DATAMART] Parada completada.");
        }
    }
}