
package livedu.api.controller;

import org.neo4j.driver.Driver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final Driver driver;
    public HealthController(Driver driver) { this.driver = driver; }

    @GetMapping
    public ResponseEntity<String> health() {
        try (var s = driver.session()) {
            s.run("RETURN 1 AS ok").consume();
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("DOWN: " + e.getMessage());
        }
    }
}
