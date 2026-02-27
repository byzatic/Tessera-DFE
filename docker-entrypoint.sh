#!/usr/bin/env bash
set -euo pipefail

CONFIG_DIR_PATH="/app/configurations"
SOURCE_ZIP_DIR="/app/data/source_zip"
PROJECTS_DIR="/app/data/projects"

WATCH_DIRS=("${SOURCE_ZIP_DIR}" "${CONFIG_DIR_PATH}")
DATA_DIR_WATCH_INTERVAL="${DATA_DIR_WATCH_INTERVAL:-60}"
IS_ENABLE_WATCH="${IS_ENABLE_WATCH:-True}"   # Default = True

JAR_PATH="/app/app.jar"

# App-level params (do not set defaults here — add only if provided)
GRAPH_CALCULATION_CRON_CYCLE="${GRAPH_CALCULATION_CRON_CYCLE-}"
INITIALIZE_STORAGE_BY_REQUEST="${INITIALIZE_STORAGE_BY_REQUEST-}"
PROMETHEUS_URI="${PROMETHEUS_URI-}"
JVM_METRICS_ENABLED="${JVM_METRICS_ENABLED-}"
PUBLISH_NODE_PIPELINE_EXECUTION_TIME="${PUBLISH_NODE_PIPELINE_EXECUTION_TIME-}"
PUBLISH_STORAGE_ANALYTICS="${PUBLISH_STORAGE_ANALYTICS-}"
PROJECT_NAME="${PROJECT_NAME-}"
CONFIG_PATH="${CONFIG_PATH-}"
DATA_DIRECTORY="${DATA_DIRECTORY-}"

JAVA_OPTS=(
  -server
  -Xms"${XMS:-512m}"
  -Xmx"${XMX:-1024m}"
)

APP_PID=""

add_sysprop_if_set() {
  local prop_name="$1"
  local prop_value="${2:-}"
  if [[ -n "$prop_value" ]]; then
    JAVA_OPTS+=("-D${prop_name}=${prop_value}")
  fi
}

build_java_opts() {
  add_sysprop_if_set "configFilePath" "${CONFIG_PATH}"
  add_sysprop_if_set "dataDirectory" "${DATA_DIRECTORY}"
  add_sysprop_if_set "projectName" "${PROJECT_NAME}"
  add_sysprop_if_set "graphCalculationCronCycle" "${GRAPH_CALCULATION_CRON_CYCLE}"
  add_sysprop_if_set "initializeStorageByRequest" "${INITIALIZE_STORAGE_BY_REQUEST}"
  add_sysprop_if_set "prometheusURI" "${PROMETHEUS_URI}"
  add_sysprop_if_set "jvmMetricsEnabled" "${JVM_METRICS_ENABLED}"
  add_sysprop_if_set "publishNodePipelineExecutionTime" "${PUBLISH_NODE_PIPELINE_EXECUTION_TIME}"
  add_sysprop_if_set "publishStorageAnalytics" "${PUBLISH_STORAGE_ANALYTICS}"
}

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
    local zip_name folder_name extract_path
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

    for _ in {1..15}; do
      kill -0 "$pid" 2>/dev/null || return 0
      sleep 1
    done

    echo "[WARN] App did not stop in time, SIGKILL pid=$pid"
    kill -9 "$pid" 2>/dev/null || true
  fi
}

run_app_background() {
  java "${JAVA_OPTS[@]}" -jar "$JAR_PATH" &
  APP_PID=$!
  echo "[INFO] Started app pid=$APP_PID"
}

run_app_foreground() {
  echo "[INFO] Starting app in foreground (watch disabled)"
  exec java "${JAVA_OPTS[@]}" -jar "$JAR_PATH"
}

watch_loop() {
  echo "[INFO] Watch enabled. Watching: ${WATCH_DIRS[*]}"
  local current_hash new_hash tick elapsed
  current_hash="$(calc_hash)"
  tick=1
  elapsed=0

  run_app_background

  while true; do
    if [[ -n "${APP_PID:-}" ]] && ! kill -0 "$APP_PID" 2>/dev/null; then
      echo "[ERROR] App pid=$APP_PID exited. Exiting."
      exit 1
    fi

    sleep "$tick"
    elapsed=$((elapsed + tick))

    if (( elapsed >= DATA_DIR_WATCH_INTERVAL )); then
      elapsed=0
      new_hash="$(calc_hash)"
      if [[ "$new_hash" != "$current_hash" ]]; then
        echo "[INFO] Changes detected, restarting app..."
        current_hash="$new_hash"
        stop_app "${APP_PID:-}"
        unzip_all_zips "$SOURCE_ZIP_DIR" "$PROJECTS_DIR"
        run_app_background
      fi
    fi
  done
}

main() {
  echo "[INFO] Startup"
  build_java_opts

  unzip_all_zips "$SOURCE_ZIP_DIR" "$PROJECTS_DIR"

  if [[ "${IS_ENABLE_WATCH,,}" == "true" ]]; then
    watch_loop
  else
    run_app_foreground
  fi
}

trap 'stop_app "${APP_PID:-}"; exit 0' SIGTERM SIGINT

main "$@"