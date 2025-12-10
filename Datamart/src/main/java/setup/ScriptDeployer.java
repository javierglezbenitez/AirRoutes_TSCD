
// setup/ScriptDeployer.java
package setup;
public interface ScriptDeployer {
    String deployAndRun(String host, String user, String pemPath,
                        String neoUser, String neoPass, String publicIp) throws Exception;
}
