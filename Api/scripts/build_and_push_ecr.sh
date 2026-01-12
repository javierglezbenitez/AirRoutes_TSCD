#!/usr/bin/env bash
set -euo pipefail

REGION=${AWS_REGION:-us-east-1}
REPO_NAME=${ECR_REPO_NAME:-graph-routes-api}

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REPO_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO_NAME}"

echo "🧱 Build Api/Dockerfile -> ${REPO_NAME}:latest"
docker build -t "${REPO_NAME}:latest" -f Api/Dockerfile .

echo "🔐 Login ECR ${REGION}"
aws ecr get-login-password --region "${REGION}" \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

echo "🏷️ Tag + 🚀 Push -> ${REPO_URI}:latest"
docker tag "${REPO_NAME}:latest" "${REPO_URI}:latest"
docker push "${REPO_URI}:latest"

echo "✅ Push completado: ${REPO_URI}:latest"
