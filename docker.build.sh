#!/usr/bin/env bash
# ./docker.build.sh --develop
# ./docker.build.sh --develop service-name
set -euo pipefail

# Enable BuildKit for Docker Compose v1; Docker Compose v2 already uses it.
export DOCKER_BUILDKIT="${DOCKER_BUILDKIT:-1}"
export COMPOSE_DOCKER_CLI_BUILD="${COMPOSE_DOCKER_CLI_BUILD:-1}"

# По умолчанию основной compose файл
COMPOSE_FILE="docker-compose.yml"

# Проверяем флаг --develop
for arg in "$@"; do
  if [[ "$arg" == "--develop" ]]; then
    COMPOSE_FILE="docker-compose.develop.yml"
    break
  fi
done

# Удаляем служебные флаги из списка имён сервисов. --no-cache принимается
# для обратной совместимости: локальный nonce и так инвалидирует слои проекта.
FILTERED_ARGS=()
for arg in "$@"; do
  if [[ "$arg" != "--develop" && "$arg" != "--no-cache" ]]; then
    FILTERED_ARGS+=("$arg")
  fi
done

# Не используем Compose --no-cache: в некоторых версиях Docker Desktop он также
# создаёт новый exec cache mount. Уникальный build arg инвалидирует инструкции
# проекта, не затрагивая именованный Maven cache mount.
LOCAL_BUILD_NONCE="${LOCAL_BUILD_NONCE:-$(date +%s)-$$-$RANDOM}"

if (( ${#FILTERED_ARGS[@]} > 0 )); then
  docker-compose --progress=plain -f "$COMPOSE_FILE" build \
    --build-arg "LOCAL_BUILD_NONCE=$LOCAL_BUILD_NONCE" \
    "${FILTERED_ARGS[@]}"
else
  docker-compose --progress=plain -f "$COMPOSE_FILE" build \
    --build-arg "LOCAL_BUILD_NONCE=$LOCAL_BUILD_NONCE"
fi
