
package aws.helpers;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class KeyPairHelper {
    private KeyPairHelper() {}

    /** Si existe el key en AWS, exige tener el PEM local; si no existe, lo crea y guarda el PEM. */
    public static String resolveOrCreatePem(Ec2Client ec2, String keyPairName, String pemPath) throws Exception {
        boolean existsInAws = ec2.describeKeyPairs().keyPairs().stream()
                .anyMatch(k -> keyPairName.equals(k.keyName()));
        File pemFile = new File(pemPath);

        if (existsInAws) {
            if (!pemFile.exists()) {
                throw new IllegalStateException("KeyPair '" + keyPairName + "' existe en AWS pero falta PEM local: " + pemFile.getAbsolutePath());
            }
            ensurePemPermissions(pemFile);
            System.out.println("   ✔ Usando PEM existente: " + pemFile.getAbsolutePath());
            return pemFile.getAbsolutePath();
        }

        CreateKeyPairResponse kp = ec2.createKeyPair(CreateKeyPairRequest.builder().keyName(keyPairName).build());
        byte[] pemBytes = kp.keyMaterial().getBytes();

        File parent = pemFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("No se pudo crear el directorio: " + parent.getAbsolutePath());
        }
        try (FileOutputStream fos = new FileOutputStream(pemFile)) { fos.write(pemBytes); }
        ensurePemPermissions(pemFile);
        System.out.println("   ✔ KeyPair guardado en: " + pemFile.getAbsolutePath());
        return pemFile.getAbsolutePath();
    }

    /** Intenta resolver la ruta del PEM por nombre de key: ENV o ~/.ssh/<key>.pem */
    public static String resolvePemByKeyName(String keyName) {
        String envName = System.getenv().getOrDefault("EC2_KEY_NAME", "");
        String envPath = System.getenv().getOrDefault("EC2_KEY_PATH", "");
        if (!envName.isBlank() && keyName.equals(envName) && !envPath.isBlank()) {
            File f = new File(envPath);
            if (f.exists()) return f.getAbsolutePath();
        }
        String homePath = System.getProperty("user.home") + "/.ssh/" + keyName + ".pem";
        File f2 = new File(homePath);
        return f2.exists() ? f2.getAbsolutePath() : null;
    }

    /** Fallback simple si solo tenemos keyName. */
    public static String resolvePemFromEnvOrHome(String keyName) {
        String envPath = System.getenv().getOrDefault("EC2_KEY_PATH", "");
        if (!envPath.isBlank() && new File(envPath).exists()) return envPath;
        String homePath = System.getProperty("user.home") + "/.ssh/" + keyName + ".pem";
        return new File(homePath).getAbsolutePath();
    }

    private static void ensurePemPermissions(File f) {
        f.setReadable(true, true);
        f.setWritable(true, true);
        f.setExecutable(false);
    }
}
