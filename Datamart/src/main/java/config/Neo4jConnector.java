
package config;

import org.neo4j.driver.*;

public class Neo4jConnector {
    private final Driver driver;

    public Neo4jConnector(String uri, String user, String password) {
        System.out.printf("🔗 Abriendo driver Neo4j: %s (user=%s)%n", uri, user);
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public Session getSession() {
        return driver.session();
    }

    public Driver getDriver() {
        return driver;
    }

    public void close() {
        System.out.println("🔒 Cerrando driver Neo4j...");
        driver.close();
    }
}
