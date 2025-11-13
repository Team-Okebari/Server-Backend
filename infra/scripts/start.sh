#!/usr/bin/env bash
set -euo pipefail

# ── 0️⃣ 환경 변수 설정 (local/prod) ──
ENV_TYPE=${1:-local} # 기본값은 local

# ── 1️⃣ docker compose 명령어 감지 ──
if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "ERROR: docker compose 또는 docker-compose가 설치되어 있지 않습니다." >&2
  exit 1
fi

# ── 2️⃣ 경로 및 파일 설정 ──
ROOT_DIR="$(cd "$(dirname "$0")"/../.. && pwd)"
INFRA_DATABASE_DIR="${ROOT_DIR}/infra/database"
INFRA_ELK_DIR="${ROOT_DIR}/infra/elk"
NETWORK_NAME="artbite-net"
ELK_NETWORK_NAME="elk-net"

DB_COMPOSE_FILE="${INFRA_DATABASE_DIR}/docker-compose.yml"
MAIN_COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"
ELK_COMPOSE_FILE="${INFRA_ELK_DIR}/docker-compose.yml"
PROJECT_NAME="artbite"

if [ "$ENV_TYPE" = "local" ]; then
  DB_COMPOSE_FILE="${INFRA_DATABASE_DIR}/docker-compose.local.yml"
  MAIN_COMPOSE_FILE="${ROOT_DIR}/docker-compose.local.yml"
  ELK_COMPOSE_FILE="${INFRA_ELK_DIR}/docker-compose.local.yml"
  PROJECT_NAME="artbite-local"
  echo "=== Running in LOCAL mode ==="
else
  echo "=== Running in PRODUCTION mode ==="
fi

cd "$ROOT_DIR" || exit 1

# ── 3️⃣ 네트워크 생성 ──
if ! docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  echo "Creating docker network '${NETWORK_NAME}'..."
  docker network create "${NETWORK_NAME}"
else
  echo "Docker network '${NETWORK_NAME}' exists."
fi

if ! docker network inspect "${ELK_NETWORK_NAME}" >/dev/null 2>&1; then
  echo "Creating docker network '${ELK_NETWORK_NAME}'..."
  docker network create "${ELK_NETWORK_NAME}"
else
  echo "Docker network '${ELK_NETWORK_NAME}' exists."
fi

# ── 4️⃣ ELK 스택 빌드 및 실행 ──
echo ""
echo "=== Building and Starting ELK Stack ==="
if [ -f "${ROOT_DIR}/.env" ]; then
  ${DOCKER_COMPOSE_CMD} --project-directory "${INFRA_ELK_DIR}" -f "${ELK_COMPOSE_FILE}" up -d --build --quiet-pull
fi

# ── 5️⃣ 메인 앱 스택 실행 (DB, Redis, App) ──
echo ""
echo "=== Starting Main Application Stack (DB, Redis, App) ==="
  ${DOCKER_COMPOSE_CMD} --project-name "${PROJECT_NAME}" --project-directory "${ROOT_DIR}" --env-file "${ROOT_DIR}/.env" -f "${DB_COMPOSE_FILE}" -f "${MAIN_COMPOSE_FILE}" up --build -d

echo ""
echo "모든 서비스가 성공적으로 시작되었습니다!"
