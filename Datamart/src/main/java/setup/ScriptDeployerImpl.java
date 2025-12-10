
package setup;

import remote.RemoteExecutor;

import java.io.InputStream;

public class ScriptDeployerImpl implements ScriptDeployer {
    private final RemoteExecutor remote;
    private final SetupScriptRepository scripts;

    public ScriptDeployerImpl(RemoteExecutor remote, SetupScriptRepository scripts) {
        this.remote = remote;
        this.scripts = scripts;
    }

    @Override
    public String deployAndRun(String host, String user, String pemPath,
                               String neoUser, String neoPass, String publicIp) throws Exception {
        String remoteScript = "/tmp/setup_neo4j.sh";
        try (InputStream in = scripts.loadFromClasspathOrFallback()) {
            remote.upload(host, user, pemPath, in, remoteScript);
        }
        String cmd = String.format(
                "sudo env NEO4J_USER='%s' NEO4J_PASSWORD='%s' PUBLIC_IP='%s' bash -lc 'chmod +x %s && %s'",
                neoUser, neoPass, publicIp, remoteScript, remoteScript);
        return remote.run(host, user, pemPath, cmd);
    }
}
