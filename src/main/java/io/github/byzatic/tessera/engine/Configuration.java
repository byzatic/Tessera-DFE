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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Configuration {
    private final static Logger logger = LoggerFactory.getLogger(Configuration.class);
    public final static MdcContextInterface MDC_ENGINE_CONTEXT = MdcEngineContext.newBuilder().build();
    public static final String APP_NAME = "metrics-core-gen-3";
    public static final String APP_VERSION = "1.0";

    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));

    public static final TempDirectory TEMP_DIRECTORY = new TempDirectory(Configuration.APP_NAME, Boolean.TRUE);

    public static final Path CONFIGURATION_FILE_PATH;

    //The cronExpressionString is a string that defines the schedule for periodic tasks using the cron format.
    //It typically consists of six fields separated by spaces:
    //  1. Seconds (0–59)
    //  2. Minutes (0–59)
    //  3. Hours (0–23)
    //  4. Day of Month (1–31)
    //  5. Month (1–12 or JAN–DEC)
    //  6. Day of Week (0–7 or SUN–SAT, where both 0 and 7 represent Sunday)
    //Each field can contain specific values, ranges, lists, or wildcards to set the schedule precisely. For example:
    //    - "0 0/5 * * * ?" runs every 5 minutes.
    //    - "0 15 10 * * ?" runs at 10:15 AM every day.
    //    - "0 0 12 1/5 * ?" runs every fifth day at noon.
    //In Java libraries like Quartz, the cron expression is used to specify when the task should execute, with each
    //part interpreted in the context of time units from seconds to days of the week.
    public static final String CRON_EXPRESSION_STRING;
    public static final Boolean INITIALIZE_STORAGE_BY_REQUEST;
    public static final Path DATA_DIR;

    public static final Path PROJECTS_DIR;
    public static final String PROJECT_NAME;
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

    private static String initCronExpressionString(XMLConfiguration config) {
        String result;

        String propertyCronExpressionString = System.getProperty("graphCalculationCronCycle", null);
        String configCronExpressionString = config.getString("graphCalculationCronCycle");
        String defaultCronExpressionString = "0 0/1 * * * ?";
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

    private static Path initProjectServicesPath(XMLConfiguration config) throws ConfigurationException {
        Path result;
        Path propertyServicesPath = (System.getProperty("servicesPath", null) != null) ? Paths.get(System.getProperty("servicesPath")) : null;
        Path configServicesPath = (config.getString("servicesPath") != null) ? Paths.get(config.getString("servicesPath")) : null;
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

    private static Path initWorkflowRoutinesPath(XMLConfiguration config) throws ConfigurationException {
        Path result;
        Path propertyWorkflowRoutinesPath = (System.getProperty("workflowRoutinesPath", null) != null) ? Paths.get(System.getProperty("workflowRoutinesPath")) : null;
        Path configWorkflowRoutinesPath = (config.getString("workflowRoutinesPath") != null) ? Paths.get(config.getString("workflowRoutinesPath")) : null;
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


    static {
        MdcContextInterface mdcEngineContext = MdcEngineContext.newBuilder().build();
        Logger logger = LoggerFactory.getLogger(Configuration.class);
        Configurations configs = new Configurations();
        try (AutoCloseable ignored = mdcEngineContext.use()) {
            logger.debug("Configure instance.");

            CONFIGURATION_FILE_PATH = initConfigFilePath();

            XMLConfiguration config = configs.xml(CONFIGURATION_FILE_PATH.toFile());

            CRON_EXPRESSION_STRING = initCronExpressionString(config);

            INITIALIZE_STORAGE_BY_REQUEST = initInitializeStorageByRequest(config);

            logger.debug("(default only) WORKING_DIR = {}", WORKING_DIR);

            DATA_DIR = initDataDirectory(config);

            PROJECTS_DIR = Configuration.DATA_DIR.resolve("projects");
            logger.debug("(default only) PROJECTS_DIR = {}", PROJECTS_DIR);

            PROJECT_NAME = initProjectName(config);

            PROJECT_SERVICES_PATH = initProjectServicesPath(config);

            PROJECT_WORKFLOW_ROUTINES_PATH = initWorkflowRoutinesPath(config);

            logger.debug("Configuration complete.");
        } catch (ConfigurationException ce) {
            logger.error("Exception : " + ExceptionUtils.getStackTrace(ce));
            throw new RuntimeException("Error reading configuration", ce);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
