
package application.workflow;

import org.neo4j.driver.Driver;
import config.Neo4jClientProvider;

public record Neo4jResources(Neo4jClientProvider client, Driver driver) {}
