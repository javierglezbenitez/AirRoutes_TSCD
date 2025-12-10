
package config;

import org.neo4j.driver.*;

public class Neo4jClientFactory implements Neo4jClientProvider {
    private final Driver driver;

    private Neo4jClientFactory(Driver driver) { this.driver = driver; }

    public static Neo4jClientProvider connectWithRetry(String uri, String user, String pass, long sleepMs) throws InterruptedException {
        int i = 0;
        while (true) {
            i++;
            try {
                System.out.printf("🔗 Abriendo driver Neo4j (intento %d): %s (user=%s)%n", i, uri, user);
                Driver drv = GraphDatabase.driver(uri, AuthTokens.basic(user, pass));
                try (Session s = drv.session()) { s.run("RETURN 1").consume(); }
                System.out.println(" ✔ Conexión Neo4j OK (intento " + i + ")");
                return new Neo4jClientFactory(drv);
            } catch (Exception e) {
                System.err.printf(" ⚠️ Falló la conexión Neo4j (intento %d): %s - %s%n", i, e.getClass().getSimpleName(), e.getMessage());
                Thread.sleep(sleepMs);
            }
        }
    }

    @Override
    public Driver getDriver() { return driver; }

    @Override
    public void close() {
        System.out.println("🔒 Cerrando driver Neo4j...");
        driver.close();
    }
}
