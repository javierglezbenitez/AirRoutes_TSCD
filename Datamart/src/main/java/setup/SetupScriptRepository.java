
package setup;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SetupScriptRepository {
    public InputStream loadFromClasspathOrFallback() {
        InputStream is = SetupScriptRepository.class.getResourceAsStream("/scripts/setup_neo4j.sh");
        if (is != null) {
            System.out.println("✔ Recurso encontrado en classpath (/scripts/setup_neo4j.sh)");
            return is;
        }
        System.out.println("⚠️ Recurso /scripts/setup_neo4j.sh NO encontrado. Usando fallback mínimo...");
        String fallback = "#!/usr/bin/env bash\nset -euxo pipefail\n" +
                "echo 'Instalando docker...'\n" +
                "sudo yum install -y docker; sudo systemctl enable docker; sudo systemctl start docker\n" +
                "sudo docker pull neo4j:5\n";
        return new ByteArrayInputStream(fallback.getBytes(StandardCharsets.UTF_8));
    }
}
