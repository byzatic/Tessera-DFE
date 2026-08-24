package io.github.byzatic.tessera.engine;

import io.github.byzatic.commons.TempDirectory;
import io.github.byzatic.tessera.engine.application.commons.logging.MdcEngineContext;
import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public final class Configuration {
    private final static Logger logger = LoggerFactory.getLogger(Configuration.class);
    public final static MdcContextInterface MDC_ENGINE_CONTEXT = MdcEngineContext.newBuilder().build();
    public static final String APP_NAME = "Tessera-DFE";
    public static String APP_VERSION;
    public static String SPECIFICATION_VERSION;
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));

    public static final TempDirectory TEMP_DIRECTORY = new TempDirectory(Configuration.APP_NAME, Boolean.TRUE);

    public static final Path CONFIGURATION_FILE_PATH;

    // The cronExpressionString is a string that defines the schedule for periodic tasks using the cron format.
    // It supports five or six fields separated by spaces. If only five fields are provided, the seconds field
    // is assumed to be 0. Field order:
    //   [seconds] minutes hours day-of-month month day-of-week
    // With 6 fields: sec min hour dom mon dow
    // With 5 fields:     min hour dom mon dow  (seconds defaults to 0)
    //
    // Fields and ranges:
    //  1. Seconds        (0–59)      — optional; defaults to 0 when omitted
    //  2. Minutes        (0–59)
    //  3. Hours          (0–23)
    //  4. Day of Month   (1–31)
    //  5. Month          (1–12)
    //  6. Day of Week    (0–6, where 0 = Sunday)
    //
    // Each field can contain specific values, ranges, steps, or lists:
    //   - "*"            any value
    //   - "a"            exact value (e.g., 5)
    //   - "a-b"          inclusive range (e.g., 1-5)
    //   - "*/n"          step values from the minimum (e.g., "*/15" → 0,15,30,45)
    //   - "a-b/n"        stepped range (e.g., "10-50/10" → 10,20,30,40,50)
    //   - "a,b,c"        comma-separated list
    //
    // Notes:
    //   - Day-of-Month AND Day-of-Week must both match (logical AND).
    //   - Text names (JAN–DEC, SUN–SAT) and Quartz-specific tokens ('?', 'L', 'W', '#') are NOT supported.
    //   - Ranges are inclusive; steps work with "*" or with an explicit range.
    //
    // Examples (equivalents shown with and without the seconds field):
    //   - "*/5 * * * *"          runs every 5 minutes at second 0.
    //   - "0 */5 * * * *"        runs every 5 minutes with seconds explicitly set.
    //   - "15 10 * * *"          runs at 10:15 AM every day.
    //   - "0 15 10 * * *"        runs at 10:15:00 AM every day.
    //   - "0 12 1/5 * *"         runs every fifth day of the month at 12:00:00.
    //   - "0 0 12 * * 1-5"       runs at 12:00:00 Monday–Friday (0=Sun, 1=Mon, …, 6=Sat).
    //   - "* * * * * *"          runs every second.
    public static volatile String CRON_EXPRESSION_STRING;
    public static volatile Boolean INITIALIZE_STORAGE_BY_REQUEST;
    public static volatile Path DATA_DIR;
    public static volatile String PROJECT_NAME;
    public static volatile Path PROJECT_ARCHIVE_PATH;
    public static volatile Path PROJECT_STAGING_DIRECTORY;
    public static volatile Duration PROJECT_WATCH_INTERVAL;
    public static volatile Duration PROJECT_INITIAL_REVISION_TIMEOUT;
    public static volatile Duration PROJECT_STARTUP_TIMEOUT;
    public static volatile Duration PROJECT_SHUTDOWN_TIMEOUT;
    public static volatile URI PROMETHEUS_URI;
    public static volatile Boolean JVM_METRICS_ENABLED;
    public static volatile Boolean PUBLISH_NODE_PIPELINE_EXECUTION_TIME;
    public static volatile Boolean PUBLISH_STORAGE_ANALYTICS;

    private Configuration() {
    }

    private static Path initConfigFilePath() throws ConfigurationException {
        Path result;

        Path propertyConfigFilePath = (System.getProperty("configFilePath", null) != null) ? Paths.get(System.getProperty("configFilePath")) : null;
        Path defaultConfigFilePath = Paths.get(Configuration.WORKING_DIR.resolve("configurations").resolve("configuration.xml").toString());

        if (propertyConfigFilePath != null) {
            result = propertyConfigFilePath;
            logger.debug("(property) CONFIGURATION_FILE_PATH = {}", propertyConfigFilePath);
        } else if (Files.exists(defaultConfigFilePath)) {
            result = defaultConfigFilePath;
            logger.debug("(default) CONFIGURATION_FILE_PATH = {}", defaultConfigFilePath);
        } else {
            throw new ConfigurationException("Configuration file path not set");
        }
        return result;
    }

    private static String initCronExpressionString(XMLConfiguration config) {
        String result;

        String propertyCronExpressionString = System.getProperty("graphCalculationCronCycle", null);
        String configCronExpressionString = config.getString("graphCalculationCronCycle");
        String defaultCronExpressionString = "0 0/1 * * * *";
        if (propertyCronExpressionString != null) {
            result = propertyCronExpressionString;
            logger.debug("(property) CRON_EXPRESSION_STRING = {}", propertyCronExpressionString);
        } else if (configCronExpressionString != null) {
            result = configCronExpressionString;
            logger.debug("(config) CRON_EXPRESSION_STRING = {}", configCronExpressionString);
        } else {
            result = defaultCronExpressionString;
            logger.debug("(default) CRON_EXPRESSION_STRING = {}", defaultCronExpressionString);
        }
        return result;
    }

    private static Boolean initInitializeStorageByRequest(XMLConfiguration config) {
        Boolean result;
        Boolean propertyInitializeStorageByRequest = (System.getProperty("initializeStorageByRequest", null) != null) ? Boolean.valueOf(System.getProperty("initializeStorageByRequest")) : null;
        Boolean configInitializeStorageByRequest = (config.getString("initializeStorageByRequest") != null) ? Boolean.valueOf(config.getString("initializeStorageByRequest")) : null;
        Boolean defaultInitializeStorageByRequest = Boolean.FALSE;
        if (propertyInitializeStorageByRequest != null) {
            result = propertyInitializeStorageByRequest;
            logger.debug("(property) INITIALIZE_STORAGE_BY_REQUEST = {}", propertyInitializeStorageByRequest);
        } else if (configInitializeStorageByRequest != null) {
            result = configInitializeStorageByRequest;
            logger.debug("(config) INITIALIZE_STORAGE_BY_REQUEST = {}", configInitializeStorageByRequest);
        } else {
            result = defaultInitializeStorageByRequest;
            logger.debug("(default) INITIALIZE_STORAGE_BY_REQUEST = {}", defaultInitializeStorageByRequest);
        }
        return result;
    }

    private static Path initDataDirectory(XMLConfiguration config) throws ConfigurationException {
        Path result;
        Path propertyDataDirectory = (System.getProperty("dataDirectory", null) != null) ? Paths.get(System.getProperty("dataDirectory")) : null;
        Path configDataDirectory = (config.getString("dataDirectory") != null) ? Paths.get(config.getString("dataDirectory")) : null;
        Path defaultDataDirectory = Paths.get(Configuration.WORKING_DIR.resolve("data").toString());

        if (propertyDataDirectory != null) {
            if (!Files.exists(propertyDataDirectory))
                throw new ConfigurationException("Property dataDirectory not exists. dataDirectory= " + propertyDataDirectory);
            result = propertyDataDirectory;
            logger.debug("(property) DATA_DIR = {}", propertyDataDirectory);
        } else if (configDataDirectory != null) {
            if (!Files.exists(configDataDirectory))
                throw new ConfigurationException("Config dataDirectory not exists. dataDirectory= " + configDataDirectory);
            result = configDataDirectory;
            logger.debug("(config) DATA_DIR = {}", configDataDirectory);
        } else {
            if (!Files.exists(defaultDataDirectory))
                throw new ConfigurationException("Default dataDirectory not exists. dataDirectory= " + defaultDataDirectory);
            result = defaultDataDirectory;
            logger.debug("(default) DATA_DIR = {}", defaultDataDirectory);
        }
        return result;
    }

    private static String initProjectName(XMLConfiguration config) throws ConfigurationException {
        String result;
        String propertyProjectName = System.getProperty("projectName", null);
        String configProjectName = config.getString("projectName");

        if (propertyProjectName != null) {
            result = propertyProjectName;
            logger.debug("(property) PROJECT_NAME = {}", propertyProjectName);
        } else if (configProjectName != null) {
            result = configProjectName;
            logger.debug("(config) PROJECT_NAME = {}", configProjectName);
        } else {
            throw new ConfigurationException("projectName is not set.");
        }
        return result;
    }

    private static URI initPrometheusURI(XMLConfiguration config) throws ConfigurationException {
        URI result;
        URI propertyPrometheusURI = (System.getProperty("prometheusURI", null) != null) ? URI.create(System.getProperty("prometheusURI")) : null;
        URI configPrometheusURI = (config.getString("prometheusURI") != null) ? URI.create(config.getString("prometheusURI")) : null;
        URI defaultPrometheusURI = URI.create("http://0.0.0.0:9090/metrics");

        if (propertyPrometheusURI != null) {
            // TODO: some checks for URI
            result = propertyPrometheusURI;
            logger.debug("(property) PROMETHEUS_URI = {}", propertyPrometheusURI);
        } else if (configPrometheusURI != null) {
            // TODO: some checks for URI
            result = configPrometheusURI;
            logger.debug("(config) PROMETHEUS_URI = {}", configPrometheusURI);
        } else {
            // TODO: some checks for URI
            result = defaultPrometheusURI;
            logger.debug("(default) PROMETHEUS_URI = {}", defaultPrometheusURI);
        }
        return result;
    }

    private static Boolean initJvmMetricsEnabled(XMLConfiguration config) throws ConfigurationException {
        Boolean result;
        Boolean propertyJvmMetricsEnabled = (System.getProperty("jvmMetricsEnabled", null) != null) ? Boolean.valueOf(System.getProperty("jvmMetricsEnabled")) : null;
        Boolean configJvmMetricsEnabled = (config.getString("jvmMetricsEnabled") != null) ? Boolean.valueOf(config.getString("jvmMetricsEnabled")) : null;
        Boolean defaultJvmMetricsEnabled = Boolean.FALSE;

        if (propertyJvmMetricsEnabled != null) {
            // TODO: some checks for URI
            result = propertyJvmMetricsEnabled;
            logger.debug("(property) JVM_METRICS_ENABLED = {}", propertyJvmMetricsEnabled);
        } else if (configJvmMetricsEnabled != null) {
            // TODO: some checks for URI
            result = configJvmMetricsEnabled;
            logger.debug("(config) JVM_METRICS_ENABLED = {}", configJvmMetricsEnabled);
        } else {
            // TODO: some checks for URI
            result = defaultJvmMetricsEnabled;
            logger.debug("(default) JVM_METRICS_ENABLED = {}", defaultJvmMetricsEnabled);
        }
        return result;
    }

    private static Boolean initPublishNodePipelineExecutionTime(XMLConfiguration config) throws ConfigurationException {
        Boolean result;
        Boolean propertyPublishNodePipelineExecutionTime = (System.getProperty("publishNodePipelineExecutionTime", null) != null) ? Boolean.valueOf(System.getProperty("publishNodePipelineExecutionTime")) : null;
        Boolean configPublishNodePipelineExecutionTime = (config.getString("publishNodePipelineExecutionTime") != null) ? Boolean.valueOf(config.getString("publishNodePipelineExecutionTime")) : null;
        Boolean defaultPublishNodePipelineExecutionTime = Boolean.FALSE;

        if (propertyPublishNodePipelineExecutionTime != null) {
            // TODO: some checks for URI
            result = propertyPublishNodePipelineExecutionTime;
            logger.debug("(property) PUBLISH_NODE_PIPELINE_EXECUTION_TIME = {}", propertyPublishNodePipelineExecutionTime);
        } else if (configPublishNodePipelineExecutionTime != null) {
            // TODO: some checks for URI
            result = configPublishNodePipelineExecutionTime;
            logger.debug("(config) PUBLISH_NODE_PIPELINE_EXECUTION_TIME = {}", configPublishNodePipelineExecutionTime);
        } else {
            // TODO: some checks for URI
            result = defaultPublishNodePipelineExecutionTime;
            logger.debug("(default) PUBLISH_NODE_PIPELINE_EXECUTION_TIME = {}", defaultPublishNodePipelineExecutionTime);
        }
        return result;
    }

    //initPublishStorageAnalytics

    private static Boolean initPublishStorageAnalytics(XMLConfiguration config) throws ConfigurationException {
        Boolean result;
        Boolean propertyPublishStorageAnalytics = (System.getProperty("publishStorageAnalytics", null) != null) ? Boolean.valueOf(System.getProperty("publishStorageAnalytics")) : null;
        Boolean configPublishStorageAnalytics = (config.getString("publishStorageAnalytics") != null) ? Boolean.valueOf(config.getString("publishStorageAnalytics")) : null;
        Boolean defaultPublishStorageAnalytics = Boolean.FALSE;

        if (propertyPublishStorageAnalytics != null) {
            // TODO: some checks for URI
            result = propertyPublishStorageAnalytics;
            logger.debug("(property) PUBLISH_STORAGE_ANALYTICS = {}", propertyPublishStorageAnalytics);
        } else if (configPublishStorageAnalytics != null) {
            // TODO: some checks for URI
            result = configPublishStorageAnalytics;
            logger.debug("(config) PUBLISH_STORAGE_ANALYTICS = {}", configPublishStorageAnalytics);
        } else {
            // TODO: some checks for URI
            result = defaultPublishStorageAnalytics;
            logger.debug("(default) PUBLISH_STORAGE_ANALYTICS = {}", defaultPublishStorageAnalytics);
        }
        return result;
    }

    public static String readSpecificationVersion() {
        String version = "UNDEFINED";
        String packageVersion = Configuration.class.getPackage().getSpecificationVersion();;
        if (packageVersion != null) {
            version= packageVersion;
        } try (var resourceVersionStream = Configuration.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (resourceVersionStream != null) {
                var props = new java.util.Properties();
                props.load(resourceVersionStream);
                String resourceVersion = props.getProperty("app.specification_version");
                if (resourceVersion != null && !resourceVersion.isBlank()) version = resourceVersion;
            }
        } catch (Exception ignored) {}
        return version;
    }

    public static String readImplementationVersion() {
        String version = "UNDEFINED";
        String packageVersion = Configuration.class.getPackage().getImplementationVersion();;
        if (packageVersion != null) {
            version= packageVersion;
        } try (var resourceVersionStream = Configuration.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (resourceVersionStream != null) {
                var props = new java.util.Properties();
                props.load(resourceVersionStream);
                String resourceVersion = props.getProperty("app.implementation_version");
                if (resourceVersion != null && !resourceVersion.isBlank()) version = resourceVersion;
            }
        } catch (Exception ignored) {}
        return version;
    }


    /**
     * Reloads all runtime settings from the configured XML file.
     *
     * <p>The new values are parsed and validated before any globally visible setting is changed.
     * Callers must stop the current engine runtime before invoking this method and must not start
     * another runtime until the method returns.</p>
     *
     * @throws ConfigurationException when the candidate configuration cannot be loaded
     */
    public static synchronized void reload() throws ConfigurationException {
        ConfigurationSnapshot candidate = loadSnapshot();
        publish(candidate);
        logger.info("Engine configuration loaded from {}", CONFIGURATION_FILE_PATH);
    }

    /**
     * Parses and validates the current configuration file without publishing its values.
     *
     * @throws ConfigurationException when the candidate configuration cannot be loaded
     */
    public static void validateCandidate() throws ConfigurationException {
        loadSnapshot();
    }

    private static ConfigurationSnapshot loadSnapshot() throws ConfigurationException {
        Configurations configurations = new Configurations();
        XMLConfiguration config = configurations.xml(CONFIGURATION_FILE_PATH.toFile());
        String cronExpression = initCronExpressionString(config);
        Boolean initializeStorageByRequest = initInitializeStorageByRequest(config);
        Path dataDirectory = initDataDirectory(config);
        String projectName = initProjectName(config);
        return new ConfigurationSnapshot(
                cronExpression,
                initializeStorageByRequest,
                dataDirectory,
                projectName,
                dataDirectory.resolve("source_zip").resolve(projectName + ".zip"),
                Paths.get(System.getProperty("java.io.tmpdir"))
                        .resolve(APP_NAME)
                        .resolve("project-revisions"),
                readPositiveDuration("projectWatchIntervalSeconds", "1"),
                readPositiveDuration("projectInitialRevisionTimeoutSeconds", "60"),
                readPositiveDuration("projectStartupTimeoutSeconds", "60"),
                readPositiveDuration("projectShutdownTimeoutSeconds", "180"),
                initPrometheusURI(config),
                initJvmMetricsEnabled(config),
                initPublishNodePipelineExecutionTime(config),
                initPublishStorageAnalytics(config)
        );
    }

    private static Duration readPositiveDuration(String propertyName, String defaultValue)
            throws ConfigurationException {
        try {
            Duration duration = Duration.ofSeconds(Long.parseLong(
                    System.getProperty(propertyName, defaultValue)
            ));
            if (duration.isZero() || duration.isNegative()) {
                throw new ConfigurationException(propertyName + " must be greater than zero");
            }
            return duration;
        } catch (NumberFormatException exception) {
            throw new ConfigurationException(
                    propertyName + " must contain a whole number of seconds",
                    exception
            );
        }
    }

    private static void publish(ConfigurationSnapshot snapshot) {
        CRON_EXPRESSION_STRING = snapshot.cronExpression;
        INITIALIZE_STORAGE_BY_REQUEST = snapshot.initializeStorageByRequest;
        DATA_DIR = snapshot.dataDirectory;
        PROJECT_NAME = snapshot.projectName;
        PROJECT_ARCHIVE_PATH = snapshot.projectArchivePath;
        PROJECT_STAGING_DIRECTORY = snapshot.projectStagingDirectory;
        PROJECT_WATCH_INTERVAL = snapshot.projectWatchInterval;
        PROJECT_INITIAL_REVISION_TIMEOUT = snapshot.projectInitialRevisionTimeout;
        PROJECT_STARTUP_TIMEOUT = snapshot.projectStartupTimeout;
        PROJECT_SHUTDOWN_TIMEOUT = snapshot.projectShutdownTimeout;
        PROMETHEUS_URI = snapshot.prometheusUri;
        JVM_METRICS_ENABLED = snapshot.jvmMetricsEnabled;
        PUBLISH_NODE_PIPELINE_EXECUTION_TIME = snapshot.publishNodePipelineExecutionTime;
        PUBLISH_STORAGE_ANALYTICS = snapshot.publishStorageAnalytics;
    }

    private static final class ConfigurationSnapshot {

        private final String cronExpression;
        private final Boolean initializeStorageByRequest;
        private final Path dataDirectory;
        private final String projectName;
        private final Path projectArchivePath;
        private final Path projectStagingDirectory;
        private final Duration projectWatchInterval;
        private final Duration projectInitialRevisionTimeout;
        private final Duration projectStartupTimeout;
        private final Duration projectShutdownTimeout;
        private final URI prometheusUri;
        private final Boolean jvmMetricsEnabled;
        private final Boolean publishNodePipelineExecutionTime;
        private final Boolean publishStorageAnalytics;

        private ConfigurationSnapshot(
                String cronExpression,
                Boolean initializeStorageByRequest,
                Path dataDirectory,
                String projectName,
                Path projectArchivePath,
                Path projectStagingDirectory,
                Duration projectWatchInterval,
                Duration projectInitialRevisionTimeout,
                Duration projectStartupTimeout,
                Duration projectShutdownTimeout,
                URI prometheusUri,
                Boolean jvmMetricsEnabled,
                Boolean publishNodePipelineExecutionTime,
                Boolean publishStorageAnalytics
        ) {
            this.cronExpression = cronExpression;
            this.initializeStorageByRequest = initializeStorageByRequest;
            this.dataDirectory = dataDirectory;
            this.projectName = projectName;
            this.projectArchivePath = projectArchivePath;
            this.projectStagingDirectory = projectStagingDirectory;
            this.projectWatchInterval = projectWatchInterval;
            this.projectInitialRevisionTimeout = projectInitialRevisionTimeout;
            this.projectStartupTimeout = projectStartupTimeout;
            this.projectShutdownTimeout = projectShutdownTimeout;
            this.prometheusUri = prometheusUri;
            this.jvmMetricsEnabled = jvmMetricsEnabled;
            this.publishNodePipelineExecutionTime = publishNodePipelineExecutionTime;
            this.publishStorageAnalytics = publishStorageAnalytics;
        }
    }

    static {
        MdcContextInterface mdcEngineContext = MdcEngineContext.newBuilder().build();
        APP_VERSION = readImplementationVersion();
        SPECIFICATION_VERSION = readSpecificationVersion();
        try (AutoCloseable ignored = mdcEngineContext.use()) {
            CONFIGURATION_FILE_PATH = initConfigFilePath();
            reload();
        } catch (ConfigurationException exception) {
            logger.error("Exception: {}", ExceptionUtils.getStackTrace(exception));
            throw new ExceptionInInitializerError(exception);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
