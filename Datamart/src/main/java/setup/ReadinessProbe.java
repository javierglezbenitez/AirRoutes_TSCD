
package setup;
public interface ReadinessProbe {
    boolean waitForBolt(String host, String user, String pemPath, int attempts, long sleepMs) throws Exception;
}
