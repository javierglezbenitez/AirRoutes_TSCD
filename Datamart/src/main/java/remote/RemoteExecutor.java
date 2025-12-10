
// remote/RemoteExecutor.java
package remote;
import java.io.InputStream;
public interface RemoteExecutor {
    void waitForSSH(String host, String user, String pemPath, long timeoutMs) throws InterruptedException;
    String run(String host, String user, String pemPath, String command) throws Exception;
    void upload(String host, String user, String pemPath, InputStream data, String remotePath) throws Exception;
}
