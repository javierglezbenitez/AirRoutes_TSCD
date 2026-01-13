package application;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppConfigTest {

    @Test
    void defaultsAndPrecedence_shouldWork() {
        Map<String,String> env = new HashMap<>();
        env.put("NEO4J_PASSWORD", "secret");
        env.put("NEO4J_URI", "bolt://foo:7687");
        env.put("NEO4J_BOLT_URI", "bolt://bar:7687");

        AppConfig cfg = new AppConfig(env);
        assertEquals("EC2", cfg.getDatamartMode());
        assertEquals("bolt://bar:7687", cfg.getNeo4jBoltUri());
    }

    @Test
    void requiresPassword() {
        Map<String,String> env = Map.of();
        assertThrows(NullPointerException.class, () -> new AppConfig(env));
    }

    @Test
    void canBeLocalMode() {
        Map<String,String> env = new HashMap<>();
        env.put("NEO4J_PASSWORD", "x");
        env.put("DATAMART_MODE", "LOCAL");

        AppConfig cfg = new AppConfig(env);
        assertEquals("LOCAL", cfg.getDatamartMode());
    }
}