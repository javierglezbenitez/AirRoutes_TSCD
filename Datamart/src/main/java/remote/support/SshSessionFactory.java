
package remote.support;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public final class SshSessionFactory {
    private SshSessionFactory() {}

    public static Session open(String host, String user, String pemPath, int timeoutMs) throws JSchException {
        if (pemPath == null || pemPath.isBlank()) {
            throw new IllegalArgumentException("pemPath vacío: necesitas EC2_KEY_PATH apuntando al .pem local.");
        }
        JSch jsch = new JSch();
        jsch.addIdentity(pemPath);
        Session session = jsch.getSession(user, host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(timeoutMs);
        return session;
    }
}
