
// aws/InstanceProvisioner.java
package aws;
public interface InstanceProvisioner {
    String ensureInstance(String name) throws Exception;
    String getPublicIp(String instanceId);
    String getKeyPairPath();
}
