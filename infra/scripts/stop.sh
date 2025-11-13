#!/usr/bin/env bash
set -euo pipefail

# ── 1️⃣ docker compose 명령어 감지 ──
if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD="docker-compose"
else
  echo "ERROR: docker compose 또는 docker-compose가 설치되어 있지 않습니다." >&2
  exit 1
fi

# ── 2️⃣ 경로 설정 ──
ROOT_DIR="$(cd "$(dirname "$0")"/../.. && pwd)"
INFRA_BASE_DIR="${ROOT_DIR}/infra"
NETWORK_NAME="artbite-net"
ELK_NETWORK_NAME="elk-net"

cd "$ROOT_DIR" || exit 1

# ── 3️⃣ compose 디렉토리 목록: 부가 인프라 서비스 병렬 종료 (ELK) ──
COMPOSE_DIRS=(
  "${INFRA_BASE_DIR}/elk"
)

# ── 4️⃣ 병렬 종료 (부가 인프라) ──
PIDS=()
for d in "${COMPOSE_DIRS[@]}"; do
  if [ -f "${d}/docker-compose.yml" ] || [ -f "${d}/docker-compose.yaml" ]; then
    echo "=== Stopping services in ${d} ==="
    pushd "${d}" >/dev/null

    if [ -f "${ROOT_DIR}/.env" ]; then
      $DOCKER_COMPOSE_CMD --env-file "${ROOT_DIR}/.env" down --timeout 10 & # --volumes 옵션은 필요시 추가
    else
      $DOCKER_COMPOSE_CMD down --timeout 10 & # --volumes 옵션은 필요시 추가
    fi

    PIDS+=($!)
    popd >/dev/null
  else
    echo "Skip: no docker-compose.yml in ${d}"
  fi
done

# ── 5️⃣ 모든 백그라운드 프로세스 대기 ──
for pid in "${PIDS[@]}"; do
  wait $pid
done

echo "부가 인프라(ELK) compose 스택이 병렬로 중지되었습니다."

# ── 6️⃣ 메인 앱 스택 종료 (DB, Redis, App) ──
echo ""
echo "=== Stopping Main Application Stack (DB, Redis, App) ==="
if [ -f "${ROOT_DIR}/.env" ]; then
  ${DOCKER_COMPOSE_CMD} --project-name artbite --project-directory "${ROOT_DIR}" --env-file "${ROOT_DIR}/.env" -f "${ROOT_DIR}/infra/database/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.yml" down
else
  ${DOCKER_COMPOSE_CMD} --project-directory "${ROOT_DIR}" -f "${ROOT_DIR}/infra/database/docker-compose.yml" -f "${ROOT_DIR}/docker-compose.yml" down
fi

# ── 7️⃣ 네트워크 삭제 ──
if docker network inspect "${NETWORK_NAME}" >/dev/null 2>&1; then
  echo "Removing docker network '${NETWORK_NAME}'..."
  docker network rm "${NETWORK_NAME}"
else
  echo "Docker network '${NETWORK_NAME}' does not exist."
fi

if docker network inspect "${ELK_NETWORK_NAME}" >/dev/null 2>&1; then
  echo "Removing docker network '${ELK_NETWORK_NAME}'..."
  docker network rm "${ELK_NETWORK_NAME}"
else
  echo "Docker network '${ELK_NETWORK_NAME}' does not exist."
fi

echo ""
echo "모든 서비스가 성공적으로 중지되었습니다."