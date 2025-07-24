# External registry host
ARG EXT_REGISTRY_HOST=10.174.18.249:5000

# Step 1: Use Maven image to build the application
FROM maven:3.8.6-eclipse-temurin-17 AS build

# Set maven package proxy
ARG MAVEN_PACKAGE_PROXY=http://10.174.18.94:8081

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies (cache this step if possible)
COPY pom.xml .

# Copy m2 configurations
COPY .m2 /app/.m2

## Download configProject dependencies (this step will be cached until pom.xml changes)
#RUN mvn dependency:go-offline

# Copy the entire configProject source code
COPY src ./src

# Build the application
ENV MAVEN_REPO_URL=${MAVEN_PACKAGE_PROXY}
RUN mvn package -DskipTests -s /app/.m2/settings.xml -U --batch-mode package -Dmaven.repo.local=.m2/repository

# Temp work dirs
RUN mkdir -p /temp_1/data/projects
RUN mkdir -p /temp_1/configurations
RUN mkdir -p /temp_1/logs



# Step 2: Use a JRE image to run the application
# TODO: Move to eclipse-temurin:17-jre-alpine
#FROM eclipse-temurin:17-jre-alpine
#FROM ${EXT_REGISTRY_HOST}/system/alpine:latest
FROM ${EXT_REGISTRY_HOST}/system/alpine:3.20.3

# Install OpenJDK JRE
RUN apk add --no-cache openjdk17-jre bash unzip

# Set the working directory inside the container
WORKDIR /app

# Copy work dirs
COPY --from=build /temp_1/data/projects /app/data/projects
COPY --from=build /temp_1/configurations /app/configurations
COPY --from=build /temp_1/logs /app/logs

# Copy the jar from the build stage
COPY --from=build /app/target/metrics-core-gen-3-*-SNAPSHOT-jar-with-dependencies.jar /app/app.jar

# Copy the docker-entrypoint
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

## Set the environment variables
ENV CONFIG_PATH="/app/configuration/configuration.xml"
ENV GRAPH_CALCULATION_CRON_CYCLE="*/10 * * * * ?"

## Run the application
ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]
