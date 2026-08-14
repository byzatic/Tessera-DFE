#!/usr/bin/env bash
set -euo pipefail

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
PROJECT_WATCH_INTERVAL_SECONDS="${PROJECT_WATCH_INTERVAL_SECONDS-}"
PROJECT_STARTUP_TIMEOUT_SECONDS="${PROJECT_STARTUP_TIMEOUT_SECONDS-}"
PROJECT_SHUTDOWN_TIMEOUT_SECONDS="${PROJECT_SHUTDOWN_TIMEOUT_SECONDS-}"

JAVA_OPTS=(
  -server
  -Xms"${XMS:-512m}"
  -Xmx"${XMX:-1024m}"
)

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
  add_sysprop_if_set "projectWatchIntervalSeconds" "${PROJECT_WATCH_INTERVAL_SECONDS}"
  add_sysprop_if_set "projectStartupTimeoutSeconds" "${PROJECT_STARTUP_TIMEOUT_SECONDS}"
  add_sysprop_if_set "projectShutdownTimeoutSeconds" "${PROJECT_SHUTDOWN_TIMEOUT_SECONDS}"
}

main() {
  echo "[INFO] Startup"
  build_java_opts
  exec java "${JAVA_OPTS[@]}" -jar "$JAR_PATH"
}

main "$@"
