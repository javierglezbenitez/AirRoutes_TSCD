
package remote;

import com.jcraft.jsch.*;
import remote.support.SshIO;
import remote.support.SshSessionFactory;

import java.io.InputStream;

public class SSHRemoteExecutor implements RemoteExecutor {

    @Override
    public void waitForSSH(String host, String user, String pemPath, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        System.out.printf("🔐 Probando SSH %s@%s ... (timeout %d ms)%n", user, host, timeoutMs);
        while (true) {
            try {
                Session s = SshSessionFactory.open(host, user, pemPath, 5000);
                s.disconnect();
                System.out.println("   ✔ SSH READY");
                break;
            } catch (JSchException e) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("   ⏳ SSH no disponible aún (elapsed " + elapsed + " ms). Error: " + e.getMessage());
                if (elapsed > timeoutMs) throw new RuntimeException("❌ SSH no disponible después de " + timeoutMs + "ms", e);
                Thread.sleep(5000);
            } catch (IllegalArgumentException e) {
                throw e;
            }
        }
    }

    @Override
    public String run(String host, String user, String pemPath, String command) throws Exception {
        System.out.printf("💻 Ejecutando remoto [%s@%s]: %s%n", user, host, command);
        Session s = SshSessionFactory.open(host, user, pemPath, 10_000);
        ChannelExec ch = (ChannelExec) s.openChannel("exec");
        ch.setCommand(command);
        ch.setErrStream(System.err);
        ch.setInputStream(null);
        ch.connect();

        String out = SshIO.drain(ch);
        System.out.println("   ✔ Comando finalizado con exitStatus=" + ch.getExitStatus());
        SshIO.close(ch, s);
        return out;
    }

    @Override
    public void upload(String host, String user, String pemPath, InputStream data, String remotePath) throws Exception {
        System.out.printf("📤 SFTP upload (stream) -> %s@%s:%s%n", user, host, remotePath);
        Session s = SshSessionFactory.open(host, user, pemPath, 10_000);
        ChannelSftp sftp = (ChannelSftp) s.openChannel("sftp");
        sftp.connect();
        sftp.put(data, remotePath, ChannelSftp.OVERWRITE);
        System.out.println("   ✔ Upload (stream) completado");
        SshIO.close(sftp, s);
    }
}
