# syntax=docker/dockerfile:1.7

FROM --platform=linux/amd64 docker.io/maven:3.8.6-eclipse-temurin-17 AS build

WORKDIR /app

ARG LOCAL_BUILD_NONCE
RUN test -n "${LOCAL_BUILD_NONCE}"

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,id=tessera-dfe-maven,target=/root/.m2/repository,sharing=locked \
    set -eu; \
    purge_snapshots() { \
      mvn dependency:purge-local-repository \
        -DsnapshotsOnly=true \
        -DreResolve=false \
        --batch-mode; \
    }; \
    purge_snapshots; \
    build_status=0; \
    mvn package -DskipTests -U --batch-mode || build_status=$?; \
    purge_status=0; \
    purge_snapshots || purge_status=$?; \
    if [ "$build_status" -ne 0 ]; then exit "$build_status"; fi; \
    exit "$purge_status"

FROM --platform=linux/amd64 docker.io/eclipse-temurin:17-jre-jammy

ARG LOCAL_BUILD_NONCE
RUN test -n "${LOCAL_BUILD_NONCE}"

RUN apt-get update && \
    apt-get install -y --no-install-recommends bash && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN mkdir -p \
    /app/data/projects \
    /app/configurations \
    /app/logs

COPY --from=build /app/target/tessera-dfe-*-jar-with-dependencies.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

ENV DATA_DIRECTORY="/app/data" \
    CONFIG_PATH="/app/configurations/configuration.xml" \
    LOGBACK_CONFIG_PATH="/app/configurations/logback.xml" \
    PROJECT_WATCH_INTERVAL_SECONDS="1" \
    PROJECT_INITIAL_REVISION_TIMEOUT_SECONDS="60" \
    PROJECT_STARTUP_TIMEOUT_SECONDS="60" \
    PROJECT_SHUTDOWN_TIMEOUT_SECONDS="180"

ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]
