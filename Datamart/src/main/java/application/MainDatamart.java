
package application;

import java.util.Map;

public class MainDatamart {
    public static void main(String[] args) throws Exception {
        AppConfig cfg = new AppConfig(System.getenv());
        DatamartOrchestrator orchestrator = new DatamartOrchestrator(cfg);
        orchestrator.run();
    }
}
