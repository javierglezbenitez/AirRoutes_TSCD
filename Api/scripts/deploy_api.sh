#!/usr/bin/env bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-us-east-1}"
REPO_NAME="${REPO_NAME:-graph-routes-api}"
API_INSTANCE_TAG_NAME="${API_INSTANCE_TAG_NAME:-graph-routes-api}"
CREATE_API="${CREATE_API:-true}"
EC2_USER="${EC2_USER:-ec2-user}"

# <<< Ajusta aquí tus credenciales de Neo4j >>>
NEO4J_PUBLIC_IP="${NEO4J_PUBLIC_IP:-100.26.41.46}"
NEO4J_USER="${NEO4J_USER:-neo4j}"
NEO4J_PASSWORD="${NEO4J_PASSWORD:-neo4j}"
# Si prefieres forzar bolt://, descomenta la siguiente línea:
# NEO4J_URI="bolt://${NEO4J_PUBLIC_IP}:7687"

SETUP_SCRIPT_LOCAL_PATH="${SETUP_SCRIPT_LOCAL_PATH:-Api/src/main/resources/setup_api.sh}"
DOCKERFILE_PATH="${DOCKERFILE_PATH:-Api/Dockerfile}"

API_IMAGE="${API_IMAGE:-}"
DOCKERHUB_USER="${DOCKERHUB_USER:-}"
DOCKERHUB_TOKEN="${DOCKERHUB_TOKEN:-}"

log(){ printf '%b\n' "$*"; }
fail(){ printf '❌ %s\n' "$*" >&2; exit 1; }
req(){ command -v "$1" >/dev/null 2>&1 || fail "No se encontró '$1' en PATH."; }

log "🔎 Validando dependencias..."
for c in aws docker mvn java ssh scp; do req "$c"; done
[[ -f "$SETUP_SCRIPT_LOCAL_PATH" ]] || fail "No existe $SETUP_SCRIPT_LOCAL_PATH (setup_api.sh)."

KEY_NAME="${REPO_NAME}-key"
PEM_PATH="$HOME/.ssh/${KEY_NAME}.pem"

log "🔐 KeyPair + .pem..."
if ! aws ec2 describe-key-pairs --key-names "$KEY_NAME" --region "$AWS_REGION" --query "KeyPairs[0].KeyName" --output text >/dev/null 2>&1; then
  log "   Creando KeyPair '$KEY_NAME' en AWS..."
  PEM_MTRL="$(aws ec2 create-key-pair --key-name "$KEY_NAME" --region "$AWS_REGION" --query "KeyMaterial" --output text)"
  mkdir -p "$(dirname "$PEM_PATH")"
  printf '%s' "$PEM_MTRL" > "$PEM_PATH"
  chmod 400 "$PEM_PATH"
  log "   ✔ Guardado pem en $PEM_PATH"
else
  log "   KeyPair '$KEY_NAME' ya existe en AWS."
  [[ -f "$PEM_PATH" ]] || fail "El .pem local NO existe."
  chmod 400 "$PEM_PATH" || true
fi

log "🧱 Compilando Api y construyendo imagen Docker local..."
mvn -q -DskipTests -pl Api -am package || fail "Maven build falló."
docker build -t "${REPO_NAME}:latest" -f "$DOCKERFILE_PATH" . || fail "Docker build falló."

if [[ -n "$DOCKERHUB_USER" && -n "${API_IMAGE}" ]]; then
  log "🐳 Publicando en Docker Hub → $API_IMAGE"
  if [[ -n "$DOCKERHUB_TOKEN" ]]; then
    echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin
  else
    docker login -u "$DOCKERHUB_USER" || true
  fi
  docker tag "${REPO_NAME}:latest" "${API_IMAGE}"
  docker push "${API_IMAGE}"
  log "   ✔ Push completado"
else
  log "ℹ️ Despliegue SIN registry (build en la EC2)."
fi

log "🖥️ Resolviendo/creando EC2 API..."
API_INSTANCE_ID="$(aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=${API_INSTANCE_TAG_NAME}" "Name=instance-state-name,Values=running" \
  --region "$AWS_REGION" --query "Reservations[0].Instances[0].InstanceId" --output text 2>/dev/null || true)"

if [[ -z "$API_INSTANCE_ID" || "$API_INSTANCE_ID" == "None" ]]; then
  if [[ "${CREATE_API}" == "true" ]]; then
    log "   No existe EC2 API running. Creando t2.micro..."
    SG_ID="$(aws ec2 create-security-group \
      --group-name "api-sg-$(date +%s)" \
      --description "SG API Graph Routes" \
      --region "$AWS_REGION" --query GroupId --output text)"
    aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 22   --cidr 0.0.0.0/0 --region "$AWS_REGION" >/dev/null
    aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 8080 --cidr 0.0.0.0/0 --region "$AWS_REGION" >/dev/null

    AMI_ID="$(aws ec2 describe-images --owners amazon \
      --filters "Name=name,Values=amzn2-ami-hvm-*-x86_64-gp2" "Name=state,Values=available" \
      --region "$AWS_REGION" --query 'Images | sort_by(@,&CreationDate)[-1].ImageId' --output text)"

    RUN_ARGS=(--image-id "$AMI_ID" --instance-type t2.micro --key-name "$KEY_NAME" \
              --security-group-ids "$SG_ID" \
              --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=${API_INSTANCE_TAG_NAME}}]")

    API_INSTANCE_ID="$(aws ec2 run-instances "${RUN_ARGS[@]}" --region "$AWS_REGION" \
      --query 'Instances[0].InstanceId' --output text)"
    log "   ✔ EC2 API creada: $API_INSTANCE_ID"
    aws ec2 wait instance-running --instance-ids "$API_INSTANCE_ID" --region "$AWS_REGION"
  else
    fail "No existe EC2 API y CREATE_API=false."
  fi
else
  log "   EC2 API running encontrada: $API_INSTANCE_ID"
fi

API_IP="$(aws ec2 describe-instances --instance-ids "$API_INSTANCE_ID" --region "$AWS_REGION" --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)"
[[ -n "$API_IP" && "$API_IP" != "None" ]] || fail "No se pudo obtener IP pública de la API."
log "   ▶ API IP: $API_IP"

log "📤 Subiendo setup_api.sh..."
scp -i "$PEM_PATH" "$SETUP_SCRIPT_LOCAL_PATH" "${EC2_USER}@${API_IP}:/tmp/setup_api.sh"

if [[ -z "$API_IMAGE" ]]; then
  APP_JAR="$(ls -1 Api/target/*.jar | head -n1 || true)"
  [[ -f "$APP_JAR" ]] || fail "No se encontró el jar en Api/target."
  log "📤 Subiendo app.jar y Dockerfile mínimo..."
  scp -i "$PEM_PATH" "$APP_JAR" "${EC2_USER}@${API_IP}:/tmp/app.jar"

  cat > /tmp/Dockerfile.local <<'EOF'
FROM eclipse-temurin:21-jre
WORKDIR /opt/app
COPY app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
EOF
  scp -i "$PEM_PATH" /tmp/Dockerfile.local "${EC2_USER}@${API_IP}:/tmp/Dockerfile.local"
  rm -f /tmp/Dockerfile.local
fi

log "💻 Ejecutando remoto setup_api.sh..."

ssh -i "$PEM_PATH" "${EC2_USER}@${API_IP}" \
"sudo bash -lc 'export AWS_REGION=\"$AWS_REGION\"; export API_IMAGE=\"$API_IMAGE\"; \
export NEO4J_PUBLIC_IP=\"$NEO4J_PUBLIC_IP\"; export NEO4J_USER=\"$NEO4J_USER\"; \
export NEO4J_PASSWORD=\"$NEO4J_PASSWORD\"; ${NEO4J_URI:+export NEO4J_URI=\"$NEO4J_URI\"; } \
chmod +x /tmp/setup_api.sh && /tmp/setup_api.sh'"

log "🔎 Verificando /api/health..."
if curl -sf "http://${API_IP}:8080/api/health" >/dev/null; then
  log "✅ API UP: http://${API_IP}:8080/api/health"
else
  log "⚠️ No responde /api/health. Revisa logs en la EC2 con: sudo docker logs -f graph-routes-api"
fi

log "🎉 Despliegue completado."
