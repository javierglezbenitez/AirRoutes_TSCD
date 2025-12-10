
package remote;

import com.jcraft.jsch.*;

import java.io.InputStream;

public class SSHRemoteExecutor implements RemoteExecutor {

    @Override
    public void waitForSSH(String host, String user, String pemPath, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        System.out.printf("🔐 Probando SSH %s@%s ... (timeout %d ms)%n", user, host, timeoutMs);
        while (true) {
            try {
                JSch jsch = new JSch();
                if (pemPath == null || pemPath.isBlank()) {
                    throw new IllegalArgumentException("pemPath vacío: necesitas EC2_KEY_PATH apuntando al .pem local.");
                }
                jsch.addIdentity(pemPath);
                Session session = jsch.getSession(user, host, 22);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(5000);
                session.disconnect();
                System.out.println("   ✔ SSH READY");
                break;
            } catch (JSchException e) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("   ⏳ SSH no disponible aún (elapsed " + elapsed + " ms). Error: " + e.getMessage());
                if (elapsed > timeoutMs) {
                    throw new RuntimeException("❌ SSH no disponible después de " + timeoutMs + "ms", e);
                }
                Thread.sleep(5000);
            } catch (IllegalArgumentException e) {
                throw e; // Falla inmediata si pemPath está vacío
            }
        }
    }

    @Override
    public String run(String host, String user, String pemPath, String command) throws Exception {
        System.out.printf("💻 Ejecutando remoto [%s@%s]: %s%n", user, host, command);
        JSch jsch = new JSch();
        jsch.addIdentity(pemPath);
        Session session = jsch.getSession(user, host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        channel.setErrStream(System.err);
        channel.setInputStream(null);
        java.io.InputStream in = channel.getInputStream();
        channel.connect();

        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read;
        while (true) {
            while ((read = in.read(buffer)) > 0) {
                output.append(new String(buffer, 0, read));
            }
            if (channel.isClosed()) break;
            Thread.sleep(100);
        }

        int exitStatus = channel.getExitStatus();
        System.out.println("   ✔ Comando finalizado con exitStatus=" + exitStatus);

        channel.disconnect();
        session.disconnect();
        return output.toString();
    }

    @Override
    public void upload(String host, String user, String pemPath, InputStream data, String remotePath) throws Exception {
        System.out.printf("📤 SFTP upload (stream) -> %s@%s:%s%n", user, host, remotePath);
        JSch jsch = new JSch();
        jsch.addIdentity(pemPath);
        Session session = jsch.getSession(user, host, 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        sftp.put(data, remotePath, ChannelSftp.OVERWRITE);
        System.out.println("   ✔ Upload (stream) completado");

        sftp.disconnect();
        session.disconnect();
    }
}
