#!/usr/bin/env bash
set -euo pipefail

CONFIG_DIR_PATH="/app/configurations"
SOURCE_ZIP_DIR="/app/data/source_zip"
PROJECTS_DIR="/app/data/projects"

WATCH_DIRS=("${SOURCE_ZIP_DIR}" "${CONFIG_DIR_PATH}")
DATA_DIR_WATCH_INTERVAL="${DATA_DIR_WATCH_INTERVAL:-60}"

JAR_PATH="/app/app.jar"

GRAPH_CALCULATION_CRON_CYCLE="${GRAPH_CALCULATION_CRON_CYCLE:-*/10 * * * * *}"
INITIALIZE_STORAGE_BY_REQUEST="${INITIALIZE_STORAGE_BY_REQUEST:-false}"
PROMETHEUS_URI="${PROMETHEUS_URI:-http://0.0.0.0:9090/metrics}"
JVM_METRICS_ENABLED="${JVM_METRICS_ENABLED:-false}"
PUBLISH_NODE_PIPELINE_EXECUTION_TIME="${PUBLISH_NODE_PIPELINE_EXECUTION_TIME:-false}"
PUBLISH_STORAGE_ANALYTICS="${PUBLISH_STORAGE_ANALYTICS:-false}"

JAVA_OPTS=(
  -server
  -Xms"${XMS:-512m}"
  -Xmx"${XMX:-1024m}"
  -DgraphCalculationCronCycle="${GRAPH_CALCULATION_CRON_CYCLE}"
  -DinitializeStorageByRequest="${INITIALIZE_STORAGE_BY_REQUEST}"
  -DprometheusURI="${PROMETHEUS_URI}"
  -DjvmMetricsEnabled="${JVM_METRICS_ENABLED}"
  -DpublishNodePipelineExecutionTime="${PUBLISH_NODE_PIPELINE_EXECUTION_TIME}"
  -DpublishStorageAnalytics="${PUBLISH_STORAGE_ANALYTICS}"
)

calc_hash() {
  {
    for d in "${WATCH_DIRS[@]}"; do
      [[ -d "$d" ]] || continue
      find "$d" -type f -print0
    done \
    | sort -z \
    | xargs -0 sha256sum 2>/dev/null \
    | sha256sum \
    | awk '{print $1}'
  } || echo "NOHASH"
}

unzip_all_zips() {
  local source_dir="$1"
  local target_dir="$2"
  [[ -d "$source_dir" ]] || { echo "[INFO] no source dir: $source_dir"; return 0; }
  mkdir -p "$target_dir"

  find "$source_dir" -maxdepth 1 -type f -name '*.zip' -print0 | while IFS= read -r -d '' zipfile; do
    zip_name="$(basename "$zipfile")"
    folder_name="${zip_name%.zip}"
    extract_path="$target_dir/$folder_name"
    rm -rf "$extract_path"
    mkdir -p "$extract_path"
    echo "[INFO] Extracting $zipfile → $extract_path"
    unzip -q "$zipfile" -d "$extract_path"
  done
}

stop_app() {
  local pid="$1"
  [[ -n "${pid:-}" ]] || return 0
  if kill -0 "$pid" 2>/dev/null; then
    echo "[INFO] Stopping app pid=$pid (SIGTERM)"
    kill "$pid" 2>/dev/null || true

    # ждём до 15 секунд
    for _ in {1..15}; do
      kill -0 "$pid" 2>/dev/null || return 0
      sleep 1
    done

    echo "[WARN] App did not stop in time, SIGKILL pid=$pid"
    kill -9 "$pid" 2>/dev/null || true
  fi
}

run_app() {
  unzip_all_zips "$SOURCE_ZIP_DIR" "$PROJECTS_DIR"
  java "${JAVA_OPTS[@]}" -jar "$JAR_PATH" &
  APP_PID=$!
  echo "[INFO] Started app pid=$APP_PID"
}

trap 'stop_app "${APP_PID:-}"; exit 0' SIGTERM SIGINT

echo "[INFO] Startup, watching: ${WATCH_DIRS[*]}"
current_hash="$(calc_hash)"
run_app

tick=1
elapsed=0

while true; do
  # JVM умерла → контейнер падает
  if [[ -n "${APP_PID:-}" ]] && ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "[ERROR] App pid=$APP_PID exited. Exiting."
    exit 1
  fi

  sleep "$tick"
  elapsed=$((elapsed + tick))

  # пора делать hash-check
  if (( elapsed >= DATA_DIR_WATCH_INTERVAL )); then
    elapsed=0
    new_hash="$(calc_hash)"
    if [[ "$new_hash" != "$current_hash" ]]; then
      echo "[INFO] Changes detected, restarting app..."
      current_hash="$new_hash"
      stop_app "${APP_PID:-}"
      run_app
    fi
  fi
done