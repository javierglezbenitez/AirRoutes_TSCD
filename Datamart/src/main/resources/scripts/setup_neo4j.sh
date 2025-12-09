#!/usr/bin/env bash
set -euxo pipefail

log() { echo -e "$@"; }

log "🚀 [setup] Comprobando/instalando Docker..."
if ! command -v docker >/dev/null 2>&1; then
  if command -v amazon-linux-extras >/dev/null 2>&1; then
    log "   [setup] Habilitando tópico docker en amazon-linux-extras..."
    sudo amazon-linux-extras enable docker || true
  fi
  log "   [setup] Instalando docker vía yum..."
  sudo yum install -y docker
  log "   [setup] Habilitando y arrancando docker (systemctl)..."
  sudo systemctl enable docker
  sudo systemctl start docker
else
  log "   [setup] Docker ya instalado."
fi

# Esperar a que el daemon responda
for i in {1..10}; do
  if sudo docker info >/dev/null 2>&1; then
    log "   ✔ docker info OK"
    break
  fi
  log "   ⏳ Esperando daemon de Docker (${i}/10)..."
  sleep 3
done

log "👤 [setup] Añadiendo ec2-user al grupo docker (opcional)..."
sudo usermod -aG docker ec2-user || true

# Directorios y permisos (uid 1000 = usuario 'neo4j' dentro del contenedor)
log "📂 [setup] Preparando /var/lib/neo4j/{data,logs} con ownership 1000:1000..."
sudo mkdir -p /var/lib/neo4j/data /var/lib/neo4j/logs
sudo chown -R 1000:1000 /var/lib/neo4j/data /var/lib/neo4j/logs

# Imagen de Neo4j
log "📥 [setup] Asegurando imagen neo4j:5 presente..."
if ! sudo docker image inspect neo4j:5 >/dev/null 2>&1; then
  sudo docker pull neo4j:5
else
  log "   [setup] Imagen neo4j:5 ya presente."
  # Si prefieres actualizar siempre a la última 5.x, descomenta lo siguiente:
  # sudo docker pull neo4j:5 || true
fi

log "🔎 [setup] Variables env: NEO4J_USER=${NEO4J_USER:-<vacío>} NEO4J_PASSWORD=${NEO4J_PASSWORD:-<vacío>} PUBLIC_IP=${PUBLIC_IP:-<vacío>}"
if [ -z "${NEO4J_USER:-}" ] || [ -z "${NEO4J_PASSWORD:-}" ]; then
  log "❌ [setup] Faltan NEO4J_USER o NEO4J_PASSWORD. Aborting."
  exit 1
fi

log "🔎 [setup] Verificando contenedor 'neo4j-datamart'..."
if sudo docker inspect neo4j-datamart >/dev/null 2>&1; then
  state=$(sudo docker inspect -f '{{.State.Status}}' neo4j-datamart || echo "unknown")
  log "   [setup] Contenedor existe. Estado: ${state}"
  case "$state" in
    running)
      log "   ✔ Contenedor ya corriendo. NO se recrea."
      ;;
    exited|created|paused|restarting|unknown)
      log "   ↪ Arrancando contenedor existente..."
      sudo docker start neo4j-datamart
      ;;
    *)
      log "   ⚠️ Estado inesperado: $state. Intentando start..."
      sudo docker start neo4j-datamart || true
      ;;
  esac
else
  log "   [setup] Contenedor no existe. Creando..."
  CID=$(sudo docker run -d \
    --name neo4j-datamart \
    --restart unless-stopped \
    -p 7474:7474 -p 7687:7687 \
    -v /var/lib/neo4j/data:/data \
    -v /var/lib/neo4j/logs:/logs \
    -e NEO4J_AUTH="${NEO4J_USER}/${NEO4J_PASSWORD}" \
    -e NEO4J_server_bolt_listen__address="0.0.0.0:7687" \
    -e NEO4J_server_http_listen__address="0.0.0.0:7474" \
    -e NEO4J_server_bolt_tls__level="DISABLED" \
    -e NEO4J_server_memory_heap_initial__size="256m" \
    -e NEO4J_server_memory_heap_max__size="512m" \
    -e NEO4J_server_memory_pagecache_size="128m" \
    neo4j:5)
  log "   🆔 docker run CID: ${CID}"

  # Si quieres anunciar IP pública (advertised), hazlo solo si es estable (Elastic IP). Evita usar una IP efímera.
  # Puedes establecer advertised address vía 'neo4j.conf' en /data/conf si lo necesitas.
fi

# Estado
log "🩺 [setup] Estado de contenedores (docker ps):"
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

# Espera a que Bolt arranque
for i in {1..12}; do
  if sudo ss -ltn | grep -q ':7687'; then
    log "   ✔ Bolt escuchando en :7687"
    break
  fi
  log "   ⏳ Esperando Bolt (${i}/12)..."
  sleep 5
done

RUNNING=$(sudo docker ps --format '{{.Names}}' | grep -w neo4j-datamart || true)
if [ -z "$RUNNING" ]; then
  log "❗ [setup] El contenedor no está corriendo. Inspección rápida:"
  sudo docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
  sudo docker inspect neo4j-datamart --format 'State={{.State.Status}} Error={{.State.Error}}' || true
  log "📝 Logs del contenedor (últimas 100 líneas):"
  sudo docker logs --tail=100 neo4j-datamart || true
fi

# IP pública (IMDS) — usa Elastic IP si quieres estabilidad
PUBLIC_IP_MD=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 || echo "${PUBLIC_IP:-localhost}")

log "✅ [setup] Neo4j (arranque verificado)."
log "🌐 HTTP:  http://${PUBLIC_IP_MD}:7474"
log "🔌 Bolt:  bolt://${PUBLIC_IP_MD}:7687"

# --------- (OPCIONAL) Rotación de contraseña si ya existe el contenedor ---------
# Requiere conocer la contraseña actual. Si la conoces, puedes ejecutar:
# sudo docker exec neo4j-datamart bash -lc \
#   "cypher-shell -u ${NEO4J_USER} -p '<PASSWORD_ACTUAL>' \"ALTER USER ${NEO4J_USER} SET PASSWORD '${NEO4J_PASSWORD}'\""
# Si NO conoces la actual y necesitas reset, hay procedimientos manuales con el archivo 'auth' en /data.
