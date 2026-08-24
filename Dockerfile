# Step 1: Use Maven image to build the application
FROM --platform=linux/amd64 docker.io/maven:3.8.6-eclipse-temurin-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and download dependencies (cache this step if possible)
COPY pom.xml .

## Download configProject dependencies (this step will be cached until pom.xml changes)
#RUN mvn dependency:go-offline

# Copy the entire configProject source code
COPY src ./src

# Build the application
RUN mvn package -DskipTests -U --batch-mode

# Temp work dirs
RUN mkdir -p /temp_1/data/projects
RUN mkdir -p /temp_1/configurations
RUN mkdir -p /temp_1/logs



# Step 2: Use a JRE image to run the application
FROM --platform=linux/amd64 docker.io/eclipse-temurin:17-jre-jammy

# Install bash for the environment-to-JVM entrypoint.
RUN apt-get update && \
    apt-get install -y --no-install-recommends bash && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory inside the container
WORKDIR /app

# Copy work dirs
COPY --from=build /temp_1/data/projects /app/data/projects
COPY --from=build /temp_1/configurations /app/configurations
COPY --from=build /temp_1/logs /app/logs

# Copy the jar from the build stage
#COPY --from=build /app/target/tessera-dfe-*-SNAPSHOT-jar-with-dependencies.jar /app/app.jar
COPY --from=build /app/target/tessera-dfe-*-jar-with-dependencies.jar /app/app.jar

# Copy the docker-entrypoint
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

## Set the environment variables
ENV DATA_DIRECTORY="/app/data"
ENV CONFIG_PATH="/app/configurations/configuration.xml"
ENV LOGBACK_CONFIG_PATH="/app/configurations/logback.xml"
ENV PROJECT_WATCH_INTERVAL_SECONDS="1"
ENV PROJECT_INITIAL_REVISION_TIMEOUT_SECONDS="60"
ENV PROJECT_STARTUP_TIMEOUT_SECONDS="60"
ENV PROJECT_SHUTDOWN_TIMEOUT_SECONDS="180"

## Run the application
ENTRYPOINT ["/bin/bash", "/app/docker-entrypoint.sh"]
