
package application.workflow;

import config.Neo4jClientProvider;
import config.Neo4jClientFactory;
import org.neo4j.driver.Driver;

public final class Neo4jClientWorkflow {
    private Neo4jClientWorkflow() {}

    public static Neo4jResources connect(String boltUri, String user, String pass, long sleepMs) throws InterruptedException {
        Neo4jClientProvider neo = Neo4jClientFactory.connectWithRetry(boltUri, user, pass, sleepMs);
        Driver driver = neo.getDriver();
        return new Neo4jResources(neo, driver);
    }
}
