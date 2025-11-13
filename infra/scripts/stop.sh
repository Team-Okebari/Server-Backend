#!/usr/bin/env bash
set -euo pipefail

ENV_TYPE=${1:-local} # 기본값 local

# ── 1️⃣ docker compose 감지 ──
if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "ERROR: docker compose가 설치되어 있지 않습니다." >&2
  exit 1
fi

# ── 2️⃣ 경로 설정 ──
ROOT_DIR="$(cd "$(dirname "$0")"/../.. && pwd)"
INFRA_BASE_DIR="${ROOT_DIR}/infra"
NETWORK_NAME="artbite-net"
ELK_NETWORK_NAME="elk-net"

DB_COMPOSE_FILE="${INFRA_BASE_DIR}/database/docker-compose.yml"
MAIN_COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"
ELK_COMPOSE_FILE="${INFRA_BASE_DIR}/elk/docker-compose.yml"
PROJECT_NAME="artbite"

if [ "$ENV_TYPE" = "local" ]; then
  DB_COMPOSE_FILE="${INFRA_BASE_DIR}/database/docker-compose.local.yml"
  MAIN_COMPOSE_FILE="${ROOT_DIR}/docker-compose.local.yml"
  ELK_COMPOSE_FILE="${INFRA_BASE_DIR}/elk/docker-compose.local.yml"
  PROJECT_NAME="artbite-local"
  echo "=== Running in LOCAL mode ==="
else
  echo "=== Running in PRODUCTION mode ==="
fi

cd "$ROOT_DIR" || exit 1

# ── 3️⃣ ELK 등 부가 인프라 병렬 종료 ──
COMPOSE_DIRS=("${INFRA_BASE_DIR}/elk")
PIDS=()

for d in "${COMPOSE_DIRS[@]}"; do
  if [ -f "${d}/docker-compose.yml" ]; then
    echo "=== Stopping services in ${d} ==="
    pushd "${d}" >/dev/null
    if [ -f "${ROOT_DIR}/.env" ]; then
      $DOCKER_COMPOSE_CMD --env-file "${ROOT_DIR}/.env" down --timeout 10 &
    else
      $DOCKER_COMPOSE_CMD down --timeout 10 &
    fi
    PIDS+=($!)
    popd >/dev/null
  fi
done

for pid in "${PIDS[@]}"; do wait $pid; done
echo "부가 인프라 스택이 병렬로 중지되었습니다."

# ── 4️⃣ 메인 앱 스택 종료 ──
echo ""
echo "=== Stopping Main Application Stack (DB, Redis, App) ==="
DOWN_OPTS="down"
if [ "$ENV_TYPE" = "local" ]; then
  DOWN_OPTS="down --volume"
fi

if [ -f "${ROOT_DIR}/.env" ]; then
  ${DOCKER_COMPOSE_CMD} --project-name "${PROJECT_NAME}" --project-directory "${ROOT_DIR}" --env-file "${ROOT_DIR}/.env" -f "${DB_COMPOSE_FILE}" -f "${MAIN_COMPOSE_FILE}" ${DOWN_OPTS}
else
  ${DOCKER_COMPOSE_CMD} --project-name "${PROJECT_NAME}" --project-directory "${ROOT_DIR}" -f "${DB_COMPOSE_FILE}" -f "${MAIN_COMPOSE_FILE}" ${DOWN_OPTS}
fi

# ── 5️⃣ 네트워크 삭제 ──
for net in "${NETWORK_NAME}" "${ELK_NETWORK_NAME}"; do
  if docker network inspect "${net}" >/dev/null 2>&1; then
    echo "Removing docker network '${net}'..."
    docker network rm "${net}"
  else
    echo "Docker network '${net}' does not exist."
  fi
done

echo ""
echo "모든 서비스가 성공적으로 중지되었습니다."
