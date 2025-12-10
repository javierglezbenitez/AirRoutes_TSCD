
package remote.support;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

import java.io.InputStream;

public final class SshIO {
    private SshIO() {}

    public static String drain(ChannelExec ch) throws Exception {
        StringBuilder output = new StringBuilder();
        byte[] buffer = new byte[1024];
        InputStream in = ch.getInputStream();
        while (true) {
            int read;
            while ((read = in.read(buffer)) > 0) {
                output.append(new String(buffer, 0, read));
            }
            if (ch.isClosed()) break;
            Thread.sleep(100);
        }
        return output.toString();
    }

    public static void close(ChannelExec ch, Session s) {
        try { ch.disconnect(); } catch (Exception ignored) {}
        try { s.disconnect(); } catch (Exception ignored) {}
    }

    public static void close(ChannelSftp sftp, Session s) {
        try { sftp.disconnect(); } catch (Exception ignored) {}
        try { s.disconnect(); } catch (Exception ignored) {}
    }
}
