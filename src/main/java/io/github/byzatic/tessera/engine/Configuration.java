package io.github.byzatic.tessera.engine;

import io.github.byzatic.commons.TempDirectory;
import io.github.byzatic.tessera.engine.application.commons.logging.MdcEngineContext;
import io.github.byzatic.tessera.engine.infrastructure.config.reader.DfeConfigLoader;
import io.github.byzatic.tessera.engine.infrastructure.config.reader.TesseraDfeConfig;
import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class Configuration {
    private final static Logger logger = LoggerFactory.getLogger(Configuration.class);
    public final static MdcContextInterface MDC_ENGINE_CONTEXT = MdcEngineContext.newBuilder().build();
    public static final String APP_NAME = "Tessera-DFE";
    public static String APP_VERSION;
    public static String SPECIFICATION_VERSION;
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));

    public static final TempDirectory TEMP_DIRECTORY = new TempDirectory(Configuration.APP_NAME, Boolean.TRUE);

    public static final Path CONFIGURATION_FILE_PATH;

    public static final String PROJECT_NAME;

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
    public static final String CRON_EXPRESSION_STRING;
    public static final Boolean INITIALIZE_STORAGE_BY_REQUEST;
    public static final Path PROJECTS_ARCHIVE_DIR;
    public static final Path PROJECTS_DIR;
    public static final Path PROJECT_SERVICES_PATH;
    public static final Path PROJECT_WORKFLOW_ROUTINES_PATH;

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

    private static String initCronExpressionString(TesseraDfeConfig config) {
        String result;

        String propertyCronExpressionString = System.getProperty("graphCalculationCronCycle", null);
        String configCronExpressionString = java.util.Optional.ofNullable(config)
                .map(c -> c.project)
                .map(p -> p.calculationCronCycle)
                .orElse(null);
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

    private static Boolean initInitializeStorageByRequest(TesseraDfeConfig config) {
        Boolean result;
        Boolean propertyInitializeStorageByRequest = (System.getProperty("initializeStorageByRequest", null) != null) ? Boolean.valueOf(System.getProperty("initializeStorageByRequest")) : null;
        Boolean configInitializeStorageByRequest = java.util.Optional.ofNullable(config)
                .map(c -> c.project)
                .map(p -> p.storages)
                .map(p -> p.initializeByRequest)
                .orElse(null);
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

    private static Path initProjectsRuntimeDir(TesseraDfeConfig config) throws ConfigurationException {
        Path result;
        Path propertyDataDirectory = (System.getProperty("projectsRuntimeDir", null) != null) ? Paths.get(System.getProperty("projectsRuntimeDir")) : null;
        Path configDataDirectory = Optional.ofNullable(config)
                .map(c -> c.projectsData)
                .map(pd -> pd.runtimeDir)
                .map(rd -> rd.path)
                .map(Paths::get)
                .orElse(null);
        Path defaultDataDirectory = Configuration.WORKING_DIR.resolve("projects_runtime_dir");

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
    private static Path initProjectsArchiveDir(TesseraDfeConfig config) throws ConfigurationException {
        Path result;
        Path propertyDataDirectory = (System.getProperty("tessera.projects.archive.dir", null) != null) ? Paths.get(System.getProperty("tessera.projects.archive.dir")) : null;
        Path configDataDirectory = Optional.ofNullable(config)
                .map(c -> c.projectsData)
                .map(pd -> pd.archiveDir)
                .map(ad -> ad.path)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Paths::get)
                .orElse(null);
        Path defaultDataDirectory = Paths.get(Configuration.WORKING_DIR.resolve("projects_archive_dir").toString());

        if (propertyDataDirectory != null) {
            if (!Files.exists(propertyDataDirectory))
                throw new ConfigurationException("Property tessera.projects.archive.dir not exists. dataDirectory= " + propertyDataDirectory);
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

    private static String initProjectName(TesseraDfeConfig config) throws ConfigurationException {
        String result;
        String propertyProjectName = System.getProperty("projectName", null);
        String configProjectName = java.util.Optional.ofNullable(config)
                .map(c -> c.project)
                .map(p -> p.name)
                .orElse(null);

        if (propertyProjectName != null) {
            result = propertyProjectName;
            logger.debug("(property) PROJECT_NAME = {}", propertyProjectName);
        } else if (configProjectName != null) {
            result = configProjectName;
            logger.debug("(config) PROJECT_NAME = {}", configProjectName);
        } else {
            throw new ConfigurationException("Project name was not set.");
        }
        return result;
    }

    private static Path initProjectServicesPath(TesseraDfeConfig config) throws ConfigurationException {
        Path result;
        Path propertyServicesPath = null;
        Path configServicesPath = null;
        Path defaultServicesPath = PROJECTS_DIR.resolve(PROJECT_NAME).resolve("modules").resolve("services");

        if (propertyServicesPath != null) {
            if (!Files.exists(propertyServicesPath))
                throw new ConfigurationException("Property servicesPath not exists. propertyServicesPath= " + propertyServicesPath);
            result = propertyServicesPath;
            logger.debug("(property) PROJECT_SERVICES_PATH = {}", propertyServicesPath);
        } else if (configServicesPath != null) {
            if (!Files.exists(configServicesPath))
                throw new ConfigurationException("Config servicesPath not exists. configServicesPath= " + configServicesPath);
            result = configServicesPath;
            logger.debug("(config) PROJECT_SERVICES_PATH = {}", configServicesPath);
        } else {
            if (!Files.exists(defaultServicesPath))
                throw new ConfigurationException("Default servicesPath not exists. defaultServicesPath= " + defaultServicesPath);
            result = defaultServicesPath;
            logger.debug("(default) PROJECT_SERVICES_PATH = {}", defaultServicesPath);
        }
        return result;
    }

    private static Path initWorkflowRoutinesPath(TesseraDfeConfig config) throws ConfigurationException {
        Path result;
        Path propertyWorkflowRoutinesPath = null;
        Path configWorkflowRoutinesPath = null;
        Path defaultWorkflowRoutinesPath = PROJECTS_DIR.resolve(PROJECT_NAME).resolve("modules").resolve("workflow_routines");

        if (propertyWorkflowRoutinesPath != null) {
            if (!Files.exists(propertyWorkflowRoutinesPath))
                throw new ConfigurationException("Property workflowRoutinesPath not exists.");
            result = propertyWorkflowRoutinesPath;
            logger.debug("(property) PROJECT_WORKFLOW_ROUTINES_PATH = {}", propertyWorkflowRoutinesPath);
        } else if (configWorkflowRoutinesPath != null) {
            if (!Files.exists(configWorkflowRoutinesPath))
                throw new ConfigurationException("Config workflowRoutinesPath not exists.");
            result = configWorkflowRoutinesPath;
            logger.debug("(config) PROJECT_WORKFLOW_ROUTINES_PATH = {}", configWorkflowRoutinesPath);
        } else {
            if (!Files.exists(defaultWorkflowRoutinesPath))
                throw new ConfigurationException("Default workflowRoutinesPath not exists.");
            result = defaultWorkflowRoutinesPath;
            logger.debug("(default) PROJECT_WORKFLOW_ROUTINES_PATH = {}", defaultWorkflowRoutinesPath);
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

    static {
        MdcContextInterface mdcEngineContext = MdcEngineContext.newBuilder().build();
        Logger logger = LoggerFactory.getLogger(Configuration.class);
        try (AutoCloseable ignored = mdcEngineContext.use()) {
            logger.debug("Configure instance.");

            APP_VERSION = readImplementationVersion();

            SPECIFICATION_VERSION = readSpecificationVersion();

            CONFIGURATION_FILE_PATH = initConfigFilePath();

            TesseraDfeConfig cfg = DfeConfigLoader.load(CONFIGURATION_FILE_PATH, Configuration.class.getResource("/schemas/tessera-dfe-configuration.xsd"));

            PROJECT_NAME = initProjectName(cfg);

            CRON_EXPRESSION_STRING = initCronExpressionString(cfg);

            INITIALIZE_STORAGE_BY_REQUEST = initInitializeStorageByRequest(cfg);

            PROJECTS_ARCHIVE_DIR = initProjectsArchiveDir(cfg);

            PROJECTS_DIR = initProjectsRuntimeDir(cfg);

            PROJECT_SERVICES_PATH = initProjectServicesPath(cfg);

            PROJECT_WORKFLOW_ROUTINES_PATH = initWorkflowRoutinesPath(cfg);

            logger.debug("Configuration complete.");
        } catch (ConfigurationException ce) {
            logger.error("Exception : " + ExceptionUtils.getStackTrace(ce));
            throw new RuntimeException("Error reading configuration", ce);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
