package application;

import aws.EC2Manager;
import aws.SSHConnector;
import config.Neo4jConnector;
import infra.S3DatalakeReader;
import repository.GraphBuilder;
import service.DataMartUpdater;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

public class MainDatamart {

    public static void main(String[] args) throws Exception {
        Map<String, String> env = System.getenv();

        // 0) Entorno
        System.out.println("🔧 Cargando variables de entorno...");
        String region = env.getOrDefault("AWS_REGION", "us-east-1");
        String bucket = env.getOrDefault("S3_BUCKET", "airroutes-datalake");
        String neoUser = env.getOrDefault("NEO4J_USER", "neo4j");
        String neoPassword = env.get("NEO4J_PASSWORD");

        System.out.printf(" - AWS_REGION=%s%n - S3_BUCKET=%s%n - NEO4J_USER=%s%n", region, bucket, neoUser);

        if (neoPassword == null || neoPassword.isBlank()) {
            throw new IllegalArgumentException("❌ Falta NEO4J_PASSWORD en el entorno");
        }

        // 1) EC2 crear o reutilizar
        System.out.println("🖥️ [1/10] Creando/reutilizando instancia EC2 'neo4j-datamart'...");
        EC2Manager ec2 = new EC2Manager(Region.of(region));
        String instanceId = ec2.getOrCreateInstance("neo4j-datamart");
        System.out.println(" ✔ Instancia: " + instanceId);

        String publicIp = ec2.getInstancePublicIp(instanceId);
        String pemPath = ec2.getKeyPairPath();
        System.out.println(" 🌍 IP pública: " + publicIp);
        System.out.println(" 🔑 PEM temporal: " + pemPath);

        // 2) Esperar SSH
        System.out.println("🔐 [2/10] Esperando disponibilidad SSH...");
        SSHConnector ssh = new SSHConnector();
        ssh.waitForSSH(publicIp, "ec2-user", pemPath, 180_000);
        System.out.println(" ✔ SSH disponible");

        // 3) Subir y ejecutar setup (instala Docker + levanta Neo4j)
        System.out.println("📦 [3/10] Preparando script setup_neo4j.sh...");
        String remoteScript = "/tmp/setup_neo4j.sh";
        InputStream scriptStream = loadSetupScriptFromClasspath();
        System.out.println(" ↗ Upload (stream) -> " + remoteScript);
        ssh.uploadStream(publicIp, "ec2-user", pemPath, scriptStream, remoteScript);

        // Ejecutar script remoto con variables de entorno
        String command = String.format(
                "sudo env NEO4J_USER='%s' NEO4J_PASSWORD='%s' PUBLIC_IP='%s' bash -lc 'chmod +x %s && %s'",
                neoUser, neoPassword, publicIp, remoteScript, remoteScript
        );
        System.out.println(" 🏃 Ejecutando script remoto con env NEO4J_USER/NEO4J_PASSWORD/PUBLIC_IP vía sudo env...");
        String scriptOutput = ssh.runCommand(publicIp, "ec2-user", pemPath, command);
        System.out.println(" 📝 Salida script:\n" + scriptOutput);
        System.out.println(" ✔ Neo4j debería estar corriendo en Docker");

        // 3.1) Diagnóstico post-setup
        System.out.println("🧪 [diag] Comprobando Docker y contenedor Neo4j...");
        String dockerVersion = ssh.runCommand(publicIp, "ec2-user", pemPath, "sudo docker version || true");
        System.out.println(" 📦 docker version:\n" + dockerVersion);

        String dockerPsA = ssh.runCommand(publicIp, "ec2-user", pemPath,
                "sudo docker ps -a --format 'table {{.Names}}\\t{{.Status}}\\t{{.Ports}}' || true");
        System.out.println(" 📋 docker ps -a:\n" + (dockerPsA.isBlank() ? "<vacio>" : dockerPsA));

        String containerState = ssh.runCommand(publicIp, "ec2-user", pemPath,
                "sudo docker inspect neo4j-datamart --format '{{.State.Status}} {{.State.Error}}' || true");
        System.out.println(" 🩺 estado contenedor: " + (containerState.isBlank() ? "<no existe>" : containerState));

        // 4) Espera a que Neo4j esté listo
        System.out.println("🩺 [4/10] Verificando readiness de Neo4j (puerto 7687)...");
        boolean ready = waitForNeo4jReady(ssh, publicIp, pemPath, 30, 5_000);
        if (!ready) {
            System.err.println(" ❌ Neo4j no está listo tras la espera. Continuaré igualmente con reintentos del driver.");
        } else {
            System.out.println(" ✔ Neo4j listo (Bolt escuchando)");
        }

        // 5) URLs
        System.out.println("🌐 [5/10] Publicando URLs de servicio...");
        String neo4jHttp = "http://" + publicIp + ":7474";
        String neo4jBolt = "bolt://" + publicIp + ":7687";
        System.out.println(" 🔎 HTTP: " + neo4jHttp);
        System.out.println(" 🔎 Bolt: " + neo4jBolt);

        // 6) Conectar a Neo4j con reintentos indefinidos
        System.out.println("🔌 [6/10] Conectando a Neo4j vía Bolt (reintentos indefinidos)...");
        Neo4jConnector neo = connectNeo4jUntilSuccess(neo4jBolt, neoUser, neoPassword, 10_000);
        GraphBuilder graph = new GraphBuilder(neo.getDriver());

        // 7) Índices/constraints
        System.out.println(" 🗃️ Creando índices/constraints si no existen...");
        try {
            graph.ensureIndexesAndConstraints();
            System.out.println(" ✔ Índices/constraints OK");
        } catch (Exception e) {
            System.err.println(" ❌ Error creando índices/constraints: " + e.getMessage());
            e.printStackTrace();
        }

        // 8) Servicio de ingesta continua (polling cada 40s)
        System.out.println("📡 [7/10] Iniciando servicio de ingesta continua (polling S3 cada 40s)...");
        Set<String> processedKeys = new HashSet<>();
        LocalDate currentDate = LocalDate.now();
        System.out.println(" 📆 Día actual: " + currentDate);

        try (S3Client s3 = S3Client.builder().region(Region.of(region)).build()) {
            S3DatalakeReader reader = new S3DatalakeReader(s3, bucket);
            DataMartUpdater updater = new DataMartUpdater(graph);

            while (true) {
                try {
                    // Cambio de fecha (medianoche)
                    LocalDate nowDate = LocalDate.now();
                    if (!nowDate.equals(currentDate)) {
                        System.out.println(" 🔄 Cambio de día detectado (" + currentDate + " -> " + nowDate + "). Reseteando estado...");
                        currentDate = nowDate;
                        processedKeys.clear();
                        System.out.println(" ✔ Estado reseteado. Nuevo prefijo S3: datalake/" + currentDate + "/");
                    }

                    // Listar claves
                    System.out.println(" 🔁 Ciclo de polling...");
                    List<String> keys = reader.listFilesForDate(currentDate);
                    System.out.println(" 🔎 Claves encontradas: " + keys.size());

                    // Detectar nuevas
                    List<String> newKeys = new ArrayList<>();
                    for (String k : keys) {
                        if (!processedKeys.contains(k)) newKeys.add(k);
                    }

                    if (newKeys.isEmpty()) {
                        System.out.println(" 💤 No hay nuevos archivos. Esperando 40s...");
                    } else {
                        System.out.println(" ➕ Nuevos archivos: " + newKeys.size());
                        for (String k : newKeys) System.out.println(" - " + k);

                        // Ingesta nuevas
                        List<Map<String, Object>> routes = reader.readSpecificKeys(newKeys);
                        System.out.println(" ✍️ Rutas nuevas a insertar: " + routes.size());
                        updater.updateToday(routes);

                        // Marcar como procesadas
                        processedKeys.addAll(newKeys);
                        System.out.println(" ✔ Marcadas como procesadas (" + processedKeys.size() + " total)");
                    }

                    Thread.sleep(40_000);

                } catch (Exception ex) {
                    System.err.println(" ❌ Error en ciclo de polling: " + ex.getMessage());
                    ex.printStackTrace();
                    Thread.sleep(20_000);
                }
            }
        }
    }

    private static boolean waitForNeo4jReady(SSHConnector ssh, String host, String pemPath, int attempts, long sleepMs) throws Exception {
        for (int i = 1; i <= attempts; i++) {
            System.out.println(" 🩺 Chequeo readiness (" + i + "/" + attempts + ")...");
            String ps = ssh.runCommand(host, "ec2-user", pemPath,
                    "sudo docker ps --format '{{.Names}} {{.Status}} {{.Ports}}' | grep -w neo4j-datamart || true");
            System.out.println(" docker ps: " + (ps.isBlank() ? "<vacio>" : ps.trim()));

            String ss = ssh.runCommand(host, "ec2-user", pemPath, "sudo ss -ltn | grep ':7687' || true");
            boolean boltListening = !ss.isBlank();
            System.out.println(" bolt listening? " + boltListening);
            if (boltListening) return true;

            Thread.sleep(sleepMs);
        }
        return false;
    }

    private static Neo4jConnector connectNeo4jUntilSuccess(String uri, String user, String pass, long sleepMs) throws InterruptedException {
        int i = 0;
        while (true) {
            i++;
            try {
                System.out.printf("🔗 Abriendo driver Neo4j (intento %d): %s (user=%s)%n", i, uri, user);
                Neo4jConnector neo = new Neo4jConnector(uri, user, pass);
                try (var session = neo.getDriver().session()) {
                    session.run("RETURN 1").consume(); // ping
                }
                System.out.println(" ✔ Conexión Neo4j OK (intento " + i + ")");
                return neo;
            } catch (Exception e) {
                System.err.printf(" ⚠️ Falló la conexión Neo4j (intento %d): %s - %s%n", i, e.getClass().getSimpleName(), e.getMessage());
                e.printStackTrace();
                Thread.sleep(sleepMs);
            }
        }
    }

    private static InputStream loadSetupScriptFromClasspath() {
        InputStream is = MainDatamart.class.getResourceAsStream("/scripts/setup_neo4j.sh");
        if (is != null) {
            System.out.println(" ✔ Recurso encontrado en classpath (/scripts/setup_neo4j.sh)");
            return is;
        }

        System.out.println(" ⚠️ Recurso /scripts/setup_neo4j.sh NO encontrado. Generando fallback en memoria...");
        String scriptContent = String.join("\n",
                "#!/bin/bash",
                "set -euxo pipefail",
                "echo \"🚀 [setup] Comprobando/instalando Docker...\"",
                "if ! command -v docker >/dev/null 2>&1; then",
                " if command -v amazon-linux-extras >/dev/null 2>&1; then",
                "   echo \" [setup] Habilitando tópico docker en amazon-linux-extras...\"",
                "   sudo amazon-linux-extras enable docker || true",
                " fi",
                " echo \" [setup] Instalando docker vía yum...\"",
                " sudo yum install -y docker",
                " sudo systemctl enable docker",
                " sudo systemctl start docker",
                "else",
                " echo \" [setup] Docker ya instalado.\"",
                "fi",
                "for i in {1..10}; do",
                " if sudo docker info >/dev/null 2>&1; then",
                "   echo \" ✔ docker info OK\"",
                "   break",
                " fi",
                " echo \" ⏳ Esperando daemon de Docker (${i}/10)...\"",
                " sleep 3",
                "done",
                "echo \"👤 [setup] Añadiendo ec2-user al grupo docker (opcional)...\"",
                "sudo usermod -aG docker ec2-user || true",
                "echo \"📥 [setup] Docker pull neo4j:5 ...\"",
                "sudo docker pull neo4j:5"
                // (Aquí puedes continuar con el resto del script de setup que tenías)
        );

        String lfContent = scriptContent.replace("\r\n", "\n").replace("\r", "\n");
        return new java.io.ByteArrayInputStream(lfContent.getBytes(StandardCharsets.UTF_8));
    }
}
