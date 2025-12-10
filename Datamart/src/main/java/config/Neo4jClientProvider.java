
// config/Neo4jClientProvider.java
package config;
import org.neo4j.driver.Driver;
public interface Neo4jClientProvider extends AutoCloseable {
    Driver getDriver();
    @Override void close();
}
