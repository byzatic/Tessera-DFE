#!/usr/bin/env bash
#
#
#

set -e
set -u

# CONFIG_PATH from the environment variables (set in the project Dockerfile)
CONFIG_DIR_PATH="/app/configurations"
SOURCE_ZIP_DIR="/app/data/source_zip"
PROJECTS_DIR="/app/data/projects"

WATCH_DIRS=("${SOURCE_ZIP_DIR}" "${CONFIG_DIR_PATH}")
# DATA_DIR_WATCH_INTERVAL - means run once every 60 seconds by default
DATA_DIR_WATCH_INTERVAL="${DATA_DIR_WATCH_INTERVAL:-60}"

JAR_PATH="/app/app.jar"
JAVA_OPTS=(
  -server
  -Xms"${XMS:-512m}"
  -Xmx"${XMX:-1024m}"
  # TODO: Exception : org.apache.commons.configuration2.ex.ConfigurationException: Property dataDirectory not exists. (default value is incorrect by quoting)
  # -DdataDirectory="${DATA_DIRECTORY:-'/app/data'}"
  # TODO: ${GRAPH_CALCULATION_CRON_CYCLE:-'*/10 * * * * ?'} default value is incorrect by quoting
  -DgraphCalculationCronCycle="${GRAPH_CALCULATION_CRON_CYCLE:-'*/10 * * * * *'}"
  # TODO: ${INITIALIZE_STORAGE_BY_REQUEST:-'false'} default value is incorrect by quoting
  -DinitializeStorageByRequest="${INITIALIZE_STORAGE_BY_REQUEST:-'false'}"
  -DprometheusURI="${PROMETHEUS_URI:-'http://0.0.0.0:9090/metrics'}"
  -DjvmMetricsEnabled="${JVM_METRICS_ENABLED:-'False'}"
  # TODO: There is no -DsharedPath arg and EmptyValueDefaults check in engine
  #-DprojectName="${DATA_DIRECTORY:-'SomeTestProject'}"
  #-DservicesPath="${DATA_DIRECTORY:-'/app/data/services'}"
  #-DworkflowRoutinesPath="${DATA_DIRECTORY:-'/app/data/workflow_routines'}"
)

echo "[INFO] Startup, watching ${WATCH_DIRS[@]}"
echo "[INFO] external args> ${*}"

# Calculate a hash for all files in the watched directory
calc_hash() {
    find "${WATCH_DIRS[@]}" -type f -exec sha256sum {} \; | sort | sha256sum | awk '{print $1}'
}

# unzip all zips
# TODO: migrate to engine
unzip_all_zips() {
  local source_dir="$1"
  local target_dir="$2"

  # [INFO] Check if source directory exists
  if [[ ! -d "$source_dir" ]]; then
    echo "[INFO] Source directory does not exist: $source_dir"
    return 1
  fi

  # [INFO] Create target directory if it doesn't exist
  if [[ ! -d "$target_dir" ]]; then
    echo "[INFO] Target directory does not exist. Creating: $target_dir"
    mkdir -p "$target_dir"
  fi

  # [INFO] Find all .zip files in the source directory (non-recursively)
  echo "[INFO] Searching for .zip files in: $source_dir"
  find "$source_dir" -maxdepth 1 -type f -name '*.zip' | while read -r zipfile; do
    zip_name="$(basename "$zipfile")"
    folder_name="${zip_name%.zip}"
    extract_path="$target_dir/$folder_name"

    # [INFO] Remove existing extraction folder if it exists
    if [[ -d "$extract_path" ]]; then
      echo "[INFO] Removing existing folder: $extract_path"
      rm -rf "$extract_path"
    fi

    # [INFO] Create extraction folder
    mkdir -p "$extract_path"

    # [INFO] Extract archive into target subdirectory
    echo "[INFO] Extracting $zipfile → $extract_path"
    unzip -q "$zipfile" -d "$extract_path"
  done

  echo "[INFO] All ZIP files extracted."
}

# running application
run() {
    unzip_all_zips ${SOURCE_ZIP_DIR} ${PROJECTS_DIR}
    java "${JAVA_OPTS[@]}" -jar "$JAR_PATH" &
    APP_PID=$!
    echo "[INFO] Started app with pid $APP_PID"
}

current_hash=$(calc_hash)

# run app
run

while true; do
    new_hash=$(calc_hash)

    if [[ "$new_hash" != "$current_hash" ]]; then
        echo "[INFO] Detected changes in ${WATCH_DIRS[@]}, restarting app..."
        current_hash="$new_hash"
        kill "$APP_PID"
        wait "$APP_PID" 2>/dev/null || true
        run
    fi

    sleep "$DATA_DIR_WATCH_INTERVAL"
done
