![Build](https://github.com/byzatic/Tessera-DFE/actions/workflows/main.yml/badge.svg)
![License](https://img.shields.io/badge/license-Apache--2.0-blue)
![Java](https://img.shields.io/badge/Java-17-orange)
![Docker Pulls](https://img.shields.io/docker/pulls/byzatic/tessera-data-flow-engine)
![Docker Version](https://img.shields.io/docker/v/byzatic/tessera-data-flow-engine)
![Prometheus](https://img.shields.io/badge/metrics-Prometheus-brightgreen)
![Architecture](https://img.shields.io/badge/architecture-DAG-blueviolet)

# Tessera Data Flow Engine
Tessera Data Flow Engine is a modular execution system based on directed acyclic graphs (DAGs), designed to build flexible and extensible data processing pipelines. Each module (service/routine) represents an independent node that processes data and passes the result along the execution graph.

---

## Документация (will be translated soon)
- [Конфигурирование Tessera DFE](.docs%2Fengine%2Fconfiguration%2FRU_README_Tessera_Configuration.md)
- [Общая структура проекта Tessera-DFE](.docs%2Fproject%2FRU_README_Main.md)
- [Observability Tessera DFE](.docs%2Fobservability%2FRU_README_Tessera_Observability.md)

---

## Getting to Know the Engine
The engine is designed to execute graph computations defined as part of a project configuration. In the current version, evaluation proceeds from the leaves toward the root, enabling incremental aggregation of results and their propagation upward through the graph structure.

At its core, the system is built around a Directed Acyclic Graph (DAG), which defines both the structure and the execution order of data processing. Computation flows bottom-up: leaf nodes retrieve data from external systems and establish the initial state, while higher-level nodes consume and process the results produced by their predecessors. Each level of the graph therefore operates on already computed values, and the direction of dependencies strictly governs the movement of data from sources to aggregating nodes.

Each graph node acts as an independent processing module containing an internal data pipeline divided into stages. A stage represents a logically complete step of processing and consists of a set of routines. Routines are small, self-contained plugin programs implemented according to the engine’s extension library. They perform specific operations on data, such as calculations, transformations, validations, filtering, or aggregation, contributing to the overall result of the stage.

During execution, routines interact with storage facilities provided by the engine. Every node has access to its own internal storage for intermediate data, as well as to a global storage shared across the entire graph. Depending on the processing logic, a routine may read from and write to either the node’s internal storage or the global storage. After a full computation cycle completes, all node-level internal storages are cleared, ensuring controlled data lifecycle management and preventing state accumulation between runs.

In addition to routines, the engine supports services—separate extensible components that are also developed using the provided plugin library. Unlike routines, services are not part of a node’s pipeline and do not directly participate in graph computation. Instead, they operate at the infrastructure level: they can read data from the global storage and perform external actions, such as publishing results, pushing data to third-party systems, or triggering integration workflows. Together, the DAG-based execution model, modular routines, and extensible services form a flexible architecture for building structured and maintainable data processing pipelines.

---

## Distribution Model

Tessera-DFE can be distributed and deployed in two primary ways, depending on the target environment and integration requirements.

The first option is container-based distribution. The engine is available as a prebuilt Docker image published on Docker Hub:
https://hub.docker.com/r/byzatic/tessera-data-flow-engine

Images are versioned and can be pulled using:
- a semantic version corresponding to the pom.xml version from the main branch,
- a tag matching a specific GitHub branch name,
- or the latest tag for the most recent stable build.

This model enables fast deployment in containerized environments and simplifies integration into CI/CD pipelines, orchestration platforms, or infrastructure-as-code workflows. It eliminates the need for local builds and ensures consistent runtime environments across installations.

The second option is source-based distribution. The engine can be cloned from GitHub as a full source project and built manually using the standard Maven build lifecycle. This approach provides maximum flexibility: it allows inspection of the codebase, custom modifications, branch-specific builds, and integration into larger systems as a library or standalone runtime. It is suitable for development environments, research scenarios, and cases where fine-grained control over dependencies or build configuration is required.

Together, these two distribution paths support both development-centric and operations-centric usage models, allowing the engine to be adopted in flexible or production-grade setups with minimal friction.

---

### Configuration and Options

### Parameter Resolution Order

Parameter resolution order:

1. (If Docker is used) **Docker Environment** / (If Docker is not used) **Java VM Options**
2. **Configuration File**
3. **Default**

Environment variables or JVM options override values from the configuration file.  
If a parameter is not defined in any layer and has no default value, the engine will fail to start.



### Parameter: configFilePath

This parameter is not required in Docker — it is predefined.  
The engine searches for `configuration.xml` inside the container at:

`/app/configurations/`

Simply mount your configuration directory to `/app/configurations/` inside the container.  
The configuration file **must be named** `configuration.xml`.

```xml
<!-- example configuration.xml -->
<Configuration>
  <graphCalculationCronCycle>*/30 * * * * *</graphCalculationCronCycle>
  <projectName>MyAwesomeProject</projectName>
  <prometheusURI>http://0.0.0.0:9090/metrics</prometheusURI>
</Configuration>
```

| Source | Value |
|--------|-------|
| Docker Environment | Not required (default path `/app/configurations/configuration.xml`) |
| Java VM Options | `-DconfigFilePath=$PWD/configuration/configuration.xml` |
| Configuration File | Not applicable (this parameter defines where the configuration file is located) |
| Default | `$PWD/configurations/configuration.xml` |



### Parameter: graphCalculationCronCycle

Cron expression defining the graph calculation interval.  
The scheduler is protected against overlapping execution.

**Format:** 5 or 6 space-separated fields  
`seconds minutes hours day-of-month month day-of-week`

If only 5 fields are provided, the seconds field defaults to `0`.

**Supported tokens per field:**  
`*` (any), exact numbers (e.g., 5), ranges (a-b), lists (a,b,c), steps (*/n), stepped ranges (a-b/n).

Names (JAN–DEC, SUN–SAT) and Quartz-specific tokens (?, L, W, #) are NOT supported.  
Day-of-Month AND Day-of-Week must both match (AND semantics).  
Day-of-Week uses 0–6, where 0 = Sunday.

**Examples:**  
`*/10 * * * * *` → every 10 seconds  
`0 */5 * * * *` → every 5 minutes (on second 0)  
`0 15 10 * * *` → 10:15:00 every day  
`0 0 12 * * 1-5` → 12:00:00 Monday–Friday (0=Sun…6=Sat)

NOTE:  
Quartz-style value `"*/10 * * * * ?"` is NOT valid (the `?` token is not supported).  
Use `"*/10 * * * * *"` to run every 10 seconds.

| Source | Value |
|--------|-------|
| Docker Environment | `GRAPH_CALCULATION_CRON_CYCLE=*/30 * * * * *` |
| Java VM Options | `-DgraphCalculationCronCycle=*/10 * * * * *` |
| Configuration File | `<graphCalculationCronCycle>*/10 * * * * *</graphCalculationCronCycle>` |
| Default | Not defined (engine startup error if missing) |



### Parameter: dataDirectory

Directory where application data are stored
The directory where projects are stored is <dataDirectory>/projects.

In the current version, only the Docker build can read a project from a `.zip` file.  
The directory for `.zip` files inside the container is:

`/app/data/source_zip`

In Docker, mount your `.zip` project directory to `/app/data/source_zip`.  
The source build requires explicit declaration of the directory.

| Source | Value |
|--------|-------|
| Docker Environment | Mount `.zip` directory to `/app/data/source_zip` |
| Java VM Options | `-DdataDirectory=/My/Awesome/Data/Directory` |
| Configuration File | `<dataDirectory>/My/Awesome/Data/Directory</dataDirectory>` |
| Default | `$PWD/data/` |



### Parameter: projectName

Project name.

In Docker, this is the `.zip` archive name.  
In source mode, this is the project directory name.

| Source | Value |
|--------|-------|
| Docker Environment | `PROJECT_NAME=MyAwesomeProject` |
| Java VM Options | `-DprojectName=MyAwesomeProject` |
| Configuration File | `<projectName>MyAwesomeProject</projectName>` |
| Default | Not defined (engine startup error if missing) |


### Parameter: initializeStorageByRequest

**[WARNING]** Highly not recommended for production use. May cause undefined behaviour. **[WARNING]**

This parameter controls lazy initialization of storages.

| Source | Value |
|--------|-------|
| Docker Environment | `INITIALIZE_STORAGE_BY_REQUEST=False` |
| Java VM Options | `-DinitializeStorageByRequest=False` |
| Configuration File | `<initializeStorageByRequest>False</initializeStorageByRequest>` |
| Default | `False` |



### Parameter: prometheusURI

HTTP endpoint where Tessera publishes Prometheus metrics (`/metrics`).

| Source | Value |
|--------|-------|
| Docker Environment | `PROMETHEUS_URI=http://0.0.0.0:9090/metrics` |
| Java VM Options | `-DprometheusURI=http://0.0.0.0:9090/metrics` |
| Configuration File | `<prometheusURI>http://0.0.0.0:9090/metrics</prometheusURI>` |
| Default | Not defined (engine startup error if missing) |



### Parameter: jvmMetricsEnabled

Enables publication of JVM metrics (heap, GC, threads, etc.) to the Prometheus endpoint.

| Source | Value |
|--------|-------|
| Docker Environment | `JVM_METRICS_ENABLED=False` |
| Java VM Options | `-DjvmMetricsEnabled=False` |
| Configuration File | `<jvmMetricsEnabled>False</jvmMetricsEnabled>` |
| Default | `False` |



### Parameter: publishNodesPipelineExecutionTime

**[WARNING]** Highly not recommended for production use. Reduces performance and causes GC pressure. **[WARNING]**

Publishes node pipeline execution time metric. Recommended only for graph debugging.

| Source | Value |
|--------|-------|
| Docker Environment | `PUBLISH_NODE_PIPELINE_EXECUTION_TIME=False` |
| Java VM Options | `-DpublishNodesPipelineExecutionTime=False` |
| Configuration File | `<publishNodesPipelineExecutionTime>False</publishNodesPipelineExecutionTime>` |
| Default | `False` |



### Parameter: publishStorageAnalytics

**[WARNING]** Highly not recommended for production use. Reduces performance and causes GC pressure. **[WARNING]**

Publishes metric for the number of items stored in storages. Recommended only for graph debugging.

| Source | Value |
|--------|-------|
| Docker Environment | `PUBLISH_STORAGE_ANALYTICS=False` |
| Java VM Options | `-DpublishStorageAnalytics=False` |
| Configuration File | `<publishStorageAnalytics>False</publishStorageAnalytics>` |
| Default | `False` |

### Prometheus Parameters Summary

- `prometheusURI` — HTTP endpoint where Tessera exposes Prometheus metrics (`/metrics`).
- `jvmMetricsEnabled` — Boolean (`True`/`False`). Enables JVM metrics publication.
- `publishNodePipelineExecutionTime` — Boolean (`True`/`False`). Publishes node pipeline execution time metric. **Reduces performance**, use for debugging only.



### Parameter: DATA_DIR_WATCH_INTERVAL (Docker Only)

Defines the polling interval (in seconds) for monitoring the directory containing `.zip` project files.  
If a `.zip` file changes, the engine will restart automatically with the updated project (**cold reload**).

| Source | Value |
|--------|-------|
| Docker Environment | `DATA_DIR_WATCH_INTERVAL=50` |
| Java VM Options | Not available outside Docker |
| Configuration File | Not available outside Docker |
| Default | Not available outside Docker |



### Parameter: IS_ENABLE_WATCH  (Docker Only)

Enables or disables directory watching for .zip project files.

If set to True, the engine monitors the project and configuration directories.
When a .zip file changes, the engine performs a cold reload (stops the current JVM and starts it again with the updated project).

If set to False, directory watching is disabled.
The engine extracts .zip files once at startup and runs the application without restart logic.

| Source | Value                  |
|--------|------------------------|
| Docker Environment | `IS_ENABLE_WATCH=True` |
| Java VM Options | `Not supported`        |
| Configuration File | `Not supported`        |
| Default | True                   |



### Parameter: XMS

`-Xms` is a JVM command-line parameter that defines the **initial heap size** at application startup.  
It specifies how much memory is allocated immediately when the JVM starts, helping to avoid allocation overhead during runtime warm-up.  
It is typically used together with `-Xmx` (maximum heap size).

| Source | Value                       |
|--------|-----------------------------|
| Docker Environment | `XMS=1024m`                 |
| Java VM Options | `-Xms1024m`                 |
| Configuration File | Not supported               |
| Default | Not defined / Docker - 512m |

### Parameter: XMX

`-Xmx` is a JVM command-line option that specifies the **maximum heap size** available to the Java application.  
The heap is the memory area where objects are allocated and where garbage collection operates.

| Source | Value         |
|--------|---------------|
| Docker Environment | `XMX=1024m`   |
| Java VM Options | `-Xmx6144m`   |
| Configuration File | Not supported |
| Default | Not defined / Docker - 1024m  |


## Running Tessera-DFE

### Running in Docker

This document describes in detail how to run Tessera-DFE using Docker and Docker Compose. The project provides two execution modes: a production-like mode based on a prebuilt image (`latest`), and a development mode that builds the image locally from the Dockerfile. Both modes rely on mounted directories for configuration, project sources, and logs, and both are controlled through the provided shell scripts.

Before starting, ensure that Docker and Docker Compose (or Docker Compose v2) are installed and available in your environment. All commands below are expected to be executed from the root directory of the repository, where the `docker-compose.yml`, `docker-compose.develop.yml`, and helper scripts are located.

The container expects a specific directory structure on the host. The `./configurations/` directory must contain the `configuration.xml` file. At runtime, this directory is mounted into the container at `/app/configurations/`, and the engine reads its configuration from `/app/configurations/configuration.xml`. The file name must be exactly `configuration.xml`, as the engine resolves it by convention unless explicitly overridden.

Project artifacts are provided as `.zip` archives. These archives must be placed into `./data/source/` on the host. This directory is mounted into the container as `/app/data/source_zip`. At startup, the entrypoint script extracts all `.zip` files into the internal projects directory and then launches the JVM. In production-like mode, directory watching can be enabled so that if any `.zip` file changes, the engine performs a cold reload: the current JVM process is stopped and restarted with the updated project.

Logs are written into `./logs/`, which is mounted into the container as `/app/logs/`. Docker log rotation is configured in the compose files using the `json-file` driver with size and file count limits to prevent uncontrolled log growth.

In production-like mode, the system uses the `docker-compose.yml` file and the public image `byzatic/tessera-data-flow-engine:latest`. Ports `8080` and `9090` are exposed. Port 9090 is typically used for the Prometheus metrics endpoint (`/metrics`). Environment variables such as `PROJECT_NAME`, `GRAPH_CALCULATION_CRON_CYCLE`, `PROMETHEUS_URI`, `JVM_METRICS_ENABLED`, `XMS`, and `XMX` are defined directly in the compose file and injected into the container. The `PROJECT_NAME` must match the name of the `.zip` archive (without the `.zip` extension), as the engine resolves the active project by this identifier.

To start the production-like environment, execute:

```bash
./docker.up.sh
```

To inspect runtime logs:

```bash
./docker.logs.sh
```

To stop the environment and remove associated containers, volumes, and images:

```bash
./docker.down.sh
```

In development mode, the system uses `docker-compose.develop.yml`. Instead of pulling a prebuilt image, it builds the image locally from the provided Dockerfile and tags it as `develop`. This mode also mounts an additional directory `./flight_recording/` into `/tmp/flight_recording/` inside the container and enables Java Flight Recorder (JFR) through `JAVA_TOOL_OPTIONS`. This allows deeper runtime diagnostics and performance analysis.

In development mode, directory watching is typically disabled (`IS_ENABLE_WATCH=False`), meaning the engine extracts `.zip` projects only once at startup and does not monitor for changes. If project sources or configuration are modified, the container must be restarted manually.

To build the development image:

```bash
./docker.build.sh --develop
```

To start the development environment:

```bash
./docker.up.sh --develop
```

To view logs:

```bash
./docker.logs.sh --develop
```

To shut down the development environment:

```bash
./docker.down.sh --develop
```

Runtime behavior is primarily controlled through environment variables defined in the compose files. These variables are translated into JVM system properties inside the container. If a variable is not defined, the corresponding `-D` option is not added to the JVM command line. Heap configuration is controlled using `XMS` and `XMX`, which map to `-Xms` and `-Xmx`. The maximum heap size must be chosen carefully based on available container memory to avoid out-of-memory conditions or aggressive garbage collection.

When directory watching is enabled (`IS_ENABLE_WATCH=True`), the entrypoint script periodically calculates a hash of the project and configuration directories. If any change is detected, it stops the running JVM gracefully, re-extracts project archives, and starts a new JVM process. This mechanism provides a controlled cold reload without rebuilding the Docker image.

From an operational perspective, production-like mode is recommended when running stable project configurations where hot reloading via directory watch is required. Development mode is recommended when actively modifying engine code, experimenting with JVM parameters, or collecting diagnostic data using JFR.

The overall execution model in Docker is deterministic: configuration is mounted, projects are extracted, JVM options are assembled from environment variables, and the engine starts either in watch mode (with automatic restarts) or in single-run mode (foreground execution). Proper separation of configuration, project artifacts, and runtime logs ensures that the container remains stateless while all operational data persists on the host.


## Build Tessera-DFE from Source

This page explains how to build Tessera-DFE locally from the GitHub sources using Maven, and what artifacts you get after the build. The build is intentionally “self-contained”: it produces runnable JARs with the correct `Main-Class` in the manifest and (depending on which artifact you choose) either bundles dependencies into a single file or ships them alongside the application JAR.

The project is published as a single Maven module with the coordinates `io.github.byzatic.tessera:tessera-dfe` and uses Java 17 as its baseline. The `pom.xml` sets `project.java.version` to `17` and configures the compiler plugin to compile with `source` and `target` set to this value. The application entry point is configured through the `fully.qualified.main.class` property and points to `${project.groupId}.engine.App`, which results in `io.github.byzatic.tessera.engine.App` as the main class.

### Prerequisites

To build from source you need a Java 17 JDK (not just a JRE) and Maven. The critical requirement is that Maven must run using Java 17, because compilation is configured for Java 17. If Maven is using an older Java runtime, compilation will fail early.

A quick check usually looks like this:

```bash
java -version
mvn -version
```

Make sure both report Java 17.

### Get the sources

Clone the repository and enter the project directory:

```bash
git clone https://github.com/byzatic/Tessera-DFE.git
cd Tessera-DFE
```

If you want a specific release version, check out the corresponding tag or branch before building. The version embedded in artifacts is taken from `pom.xml` (for example, `0.1.4`).

### Build with Maven

The standard build command is:

```bash
mvn clean package
```

If you are iterating locally and do not need tests for a particular run, you can skip them:

```bash
mvn clean package -DskipTests
```

After the build completes, Maven writes all artifacts into the `target/` directory. This project is configured to produce more than one “runnable” option, so it is normal to see multiple JARs in `target/`.

### What artifacts are produced

The build config is designed to support three practical deployment models.

#### 1) “Thin” JAR + dependencies directory (`target/lib/`)

The build uses the Maven Dependency Plugin to copy runtime dependencies into `target/lib` during the `package` phase. In addition, the Maven JAR Plugin is configured to add a `Class-Path` entry to the manifest and uses `lib/` as the classpath prefix. This model is convenient when you want a small application JAR and a separate directory with dependencies.

In this mode you typically run the main JAR from `target/` and keep `target/lib/` next to it.

#### 2) Assembly “jar-with-dependencies” (single fat JAR)

The Maven Assembly Plugin is configured with the `jar-with-dependencies` descriptor and is bound to the `package` phase. This produces a single JAR that contains the application classes and all dependencies. This is often the simplest artifact to run locally or ship to a server because you only need one file.

The filename usually contains `jar-with-dependencies`.

#### 3) Shade “shaded” JAR (single fat JAR, shaded)

The Maven Shade Plugin is also enabled and attaches a shaded artifact. Like the assembly JAR, this is a single file that contains dependencies, but it is built through shading, which can be useful if you need to handle dependency conflicts or resource merging more explicitly. The shaded artifact is attached to the build output, so you will see it in `target/` as an additional runnable JAR.

### Verify build output

After a successful build, inspect `target/` to see which runnable artifact you prefer:

```bash
ls -lah target/
```

In most setups you will find:
- the main project JAR,
- a `*-jar-with-dependencies.jar` (assembly),
- a shaded JAR (name depends on Maven’s attached artifact naming),
- and the `target/lib/` directory with copied dependencies.

### Run the engine (source build, no Docker)

When you run the engine without Docker, you typically control configuration using Java system properties (`-D...`). This mirrors the Docker runtime, where environment variables are translated into the same `-D` properties.

A common local layout is:

- `./configurations/configuration.xml`
- `./data/` (projects directory; source mode usually expects projects as directories rather than zipped archives)

#### Running the assembly fat JAR

Replace the JAR name below with the one you have in `target/`:

```bash
java   -DconfigFilePath="$PWD/configurations/configuration.xml"   -DdataDirectory="$PWD/data"   -DprojectName="MyAwesomeProject"   -jar target/tessera-dfe-0.1.4-jar-with-dependencies.jar
```

#### Running the shaded fat JAR

Again, replace the JAR name if it differs in your build:

```bash
java   -DconfigFilePath="$PWD/configurations/configuration.xml"   -DdataDirectory="$PWD/data"   -DprojectName="MyAwesomeProject"   -jar target/tessera-dfe-0.1.4-shaded.jar
```

#### Running the thin JAR + `target/lib`

If you choose the thin JAR model, keep `target/lib/` next to the JAR and run the main JAR from the `target/` directory. Since the manifest is configured with the classpath prefix `lib/`, the JVM can resolve dependencies from `target/lib/` as long as the directory structure is preserved.

### Notes about dependencies

Tessera-DFE depends on the Tessera Workflow Toolkit libraries (`tessera-enginecommon-lib`, `tessera-storageapi-lib`, `tessera-service-lib`, `tessera-workflowroutine-lib`) and a set of common Java libraries (logging via SLF4J/Logback, configuration helpers, JSON parsing via Gson, and other utilities). The build output you run determines whether you need those dependencies externally (thin JAR + `target/lib`) or they are already bundled into a single runnable file (assembly/shaded).

### Practical troubleshooting

If `mvn package` fails with compiler errors, verify that Maven is running on Java 17. If the application starts but fails early, double-check that `configuration.xml` exists at the path provided by `-DconfigFilePath`, and that `dataDirectory` and `projectName` point to an existing project in the expected directory structure.

---

# Contributing

Contributions are welcome and encouraged.

To contribute to the project:

1. Create an Issue describing the problem, idea, or enhancement.
2. Fork the repository to your GitHub account.
3. Create a new branch following the naming convention `feature/feature-issue-*`.
4. Implement your changes and submit a Pull Request.

Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contribution guidelines, coding standards, and review process information.

---

# License

This project is distributed under the [Apache-2.0](LICENSE) license.

---

# Contacts

**Author:** Svyatoslav Vlasov  
**Email:** s.vlaosv98@gmail.com  
**GitHub:** https://github.com/byzatic
