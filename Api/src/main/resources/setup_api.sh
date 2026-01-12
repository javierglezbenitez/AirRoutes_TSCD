#!/usr/bin/env bash
set -euxo pipefail

log(){ echo -e "$@"; }

# -----------------------------
# Configuración explícita Neo4j
# -----------------------------
NEO4J_URI="neo4j://54.242.215.23:7687"
NEO4J_USER="neo4j"
NEO4J_PASSWORD="Jorge_2004"

JAVA_OPTS="\
-Dspring.neo4j.uri=${NEO4J_URI} \
-Dspring.neo4j.authentication.username=${NEO4J_USER} \
-Dspring.neo4j.authentication.password=${NEO4J_PASSWORD} \
-Dspring.neo4j.security.encrypted=false \
"

# -----------------------------
# Docker
# -----------------------------
if ! command -v docker >/dev/null 2>&1; then
  sudo yum install -y docker
  sudo systemctl enable docker
  sudo systemctl start docker
fi
for i in {1..10}; do
  if sudo docker info >/dev/null 2>&1; then break; fi
  sleep 3
done
sudo usermod -aG docker ec2-user || true

CONTAINER_NAME="graph-routes-api"
EXPOSE_PORT=8080
sudo docker rm -f "$CONTAINER_NAME" || true

# -----------------------------
# Lanzamiento
# -----------------------------
[[ -f /tmp/app.jar && -f /tmp/Dockerfile.local ]] || { echo "Faltan /tmp/app.jar o /tmp/Dockerfile.local"; exit 1; }

log "🏗️ docker build local (graph-routes-api:local)"
sudo docker build -t graph-routes-api:local -f /tmp/Dockerfile.local /tmp

log "▶ docker run (local build) ${CONTAINER_NAME}"
sudo docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  -p ${EXPOSE_PORT}:8080 \
  -e JAVA_TOOL_OPTIONS="${JAVA_OPTS}" \
  -e SPRING_NEO4J_URI="${NEO4J_URI}" \
  -e SPRING_NEO4J_AUTHENTICATION_USERNAME="${NEO4J_USER}" \
  -e SPRING_NEO4J_AUTHENTICATION_PASSWORD="${NEO4J_PASSWORD}" \
  graph-routes-api:local

# Espera health
for i in {1..20}; do
  code=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:${EXPOSE_PORT}/api/health || echo "000")
  if [[ "${code}" == "200" ]]; then echo "✔ API UP (200)"; break; fi
  echo "⏳ Esperando API (${i}/20), status=${code}"
  sleep 5
done

echo "🔎 Env dentro del contenedor (resumen):"
sudo docker exec -i "${CONTAINER_NAME}" /bin/sh -lc 'env | grep -E "NEO4J_|SPRING_NEO4J|JAVA_TOOL_OPTIONS" | sort || true'

sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
