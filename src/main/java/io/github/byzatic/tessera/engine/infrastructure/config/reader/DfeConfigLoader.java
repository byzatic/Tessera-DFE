package io.github.byzatic.tessera.engine.infrastructure.config.reader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.net.URL;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import io.github.byzatic.tessera.engine.application.commons.exceptions.ConfigurationValidationException;

/**
 * Loads Tessera-DFE XML config via Apache Commons Configuration 2,
 * validates against XSD, maps to DTO and enforces conditional business rules.
 */
public final class DfeConfigLoader {

    private DfeConfigLoader() {}

    public static TesseraDfeConfig load(Path xmlPath, URL xsdUrl)
            throws ConfigurationValidationException {
        Objects.requireNonNull(xmlPath, "xmlPath");
        Objects.requireNonNull(xsdUrl, "xsdUrl");

        try {
            DocumentBuilder docBuilder = createValidatingDocumentBuilder(xsdUrl);

            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<XMLConfiguration> builder =
                    new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                            .configure(params.xml()
                                    .setFile(xmlPath.toFile())
                                    .setDocumentBuilder(docBuilder)
                                    // optional keys are allowed; we'll check presence ourselves
                                    .setThrowExceptionOnMissing(false));

            XMLConfiguration xml = builder.getConfiguration(); // parse + XSD validate

            TesseraDfeConfig dto = new TesseraDfeConfig();

            // -------- project (optional) --------
            boolean projectPresent =
                    xml.containsKey("engine.run.project.CalculationCronCycle") ||
                            xml.containsKey("engine.run.project.storages.initializeByRequest") ||
                            xml.containsKey("engine.run.project[@name]");

            if (projectPresent) {
                TesseraDfeConfig.ProjectConfig pr = new TesseraDfeConfig.ProjectConfig();
                pr.name = trimToNull(xml.getString("engine.run.project[@name]"));
                pr.calculationCronCycle = trimToNull(xml.getString("engine.run.project.CalculationCronCycle"));

                // storages is optional; initializeByRequest may be absent -> null
                if (xml.containsKey("engine.run.project.storages.initializeByRequest")) {
                    TesseraDfeConfig.StoragesConfig st = new TesseraDfeConfig.StoragesConfig();
                    st.initializeByRequest = getNullableBoolean(xml, "engine.run.project.storages.initializeByRequest");
                    pr.storages = st;
                }
                dto.project = pr;
            }

            // -------- projects-data (optional) --------
            boolean projectsDataPresent =
                    xml.containsKey("engine.projects-data.archive-dir.path") ||
                            xml.containsKey("engine.projects-data.runtime-dir.path") ||
                            xml.containsKey("engine.projects-data.runtime-dir.rotation[@enabled]") ||
                            xml.containsKey("engine.projects-data.runtime-dir.rotation[@timestamp-pattern]");

            if (projectsDataPresent) {
                TesseraDfeConfig.ProjectsDataConfig pd = new TesseraDfeConfig.ProjectsDataConfig();

                // archive-dir (optional, but if present -> path required by XSD)
                if (xml.containsKey("engine.projects-data.archive-dir.path")) {
                    TesseraDfeConfig.DirWithPathConfig ad = new TesseraDfeConfig.DirWithPathConfig();
                    ad.path = trimToNull(xml.getString("engine.projects-data.archive-dir.path"));
                    pd.archiveDir = ad;
                }

                // runtime-dir (optional, but if present -> path required by XSD; rotation optional)
                boolean runtimePresent =
                        xml.containsKey("engine.projects-data.runtime-dir.path") ||
                                xml.containsKey("engine.projects-data.runtime-dir.rotation[@enabled]") ||
                                xml.containsKey("engine.projects-data.runtime-dir.rotation[@timestamp-pattern]");

                if (runtimePresent) {
                    TesseraDfeConfig.RuntimeDirConfig rd = new TesseraDfeConfig.RuntimeDirConfig();
                    rd.path = trimToNull(xml.getString("engine.projects-data.runtime-dir.path"));

                    boolean rotationPresent =
                            xml.containsKey("engine.projects-data.runtime-dir.rotation[@enabled]") ||
                                    xml.containsKey("engine.projects-data.runtime-dir.rotation[@timestamp-pattern]");

                    if (rotationPresent) {
                        TesseraDfeConfig.RotationConfig rot = new TesseraDfeConfig.RotationConfig();
                        rot.enabled = getNullableBoolean(xml, "engine.projects-data.runtime-dir.rotation[@enabled]");
                        rot.timestampPattern = trimToNull(
                                xml.getString("engine.projects-data.runtime-dir.rotation[@timestamp-pattern]", null)
                        );
                        rd.rotation = rot;
                    }
                    pd.runtimeDir = rd;
                }

                dto.projectsData = pd;
            }

            // -------- business validation (conditional rules) --------
            validate(dto);

            return dto;

        } catch (ConfigurationValidationException e) {
            throw e;
        } catch (ConfigurationException e) {
            throw new ConfigurationValidationException("Configuration error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigurationValidationException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    // ===== helpers =====

    private static DocumentBuilder createValidatingDocumentBuilder(URL xsdUrl) throws Exception {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(xsdUrl);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setSchema(schema);
        dbf.setXIncludeAware(false);
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (Throwable ignored) {}

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new StrictErrorHandler());
        return db;
    }

    private static final class StrictErrorHandler implements ErrorHandler {
        @Override public void warning(SAXParseException e) throws SAXException { throw e; }
        @Override public void error(SAXParseException e) throws SAXException { throw e; }
        @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
    }

    private static String trimToNull(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }

    private static Boolean getNullableBoolean(XMLConfiguration xml, String key) {
        if (!xml.containsKey(key)) return null;
        // getBoolean(String, Boolean) перегрузки нет, используем строковый разбор
        String v = trimToNull(xml.getString(key));
        if (v == null) return null;
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(v);
        }
        throw new IllegalArgumentException("Invalid boolean value for '" + key + "': " + v);
    }

    private static boolean isCronLike(String cron) {
        String[] fields = cron.trim().split("\\s+");
        if (fields.length != 5 && fields.length != 6) return false;
        for (String f : fields) {
            for (String a : f.split(",")) {
                if (!a.matches("\\*")
                        && !a.matches("\\*/\\d+")
                        && !a.matches("\\d+")
                        && !a.matches("\\d+-\\d+")
                        && !a.matches("\\d+-\\d+/\\d+")) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void validate(TesseraDfeConfig c) throws ConfigurationValidationException {
        // project: optional; IF present -> CalculationCronCycle required & cron-like
        if (c.project != null) {
            if (c.project.calculationCronCycle == null || !isCronLike(c.project.calculationCronCycle)) {
                throw new ConfigurationValidationException(
                        "If <project> is present, <CalculationCronCycle> must be a valid 5- or 6-field cron."
                );
            }
            // storages may be null; initializeByRequest may be null — ок
        }

        // projects-data: optional
        if (c.projectsData != null) {
            // archive-dir: if present -> path required (XSD гарантирует, но дадим понятную ошибку)
            if (c.projectsData.archiveDir != null) {
                if (c.projectsData.archiveDir.path == null || c.projectsData.archiveDir.path.isEmpty()) {
                    throw new ConfigurationValidationException("<archive-dir><path> must be non-empty if <archive-dir> is present.");
                }
            }
            // runtime-dir: if present -> path required; rotation optional
            if (c.projectsData.runtimeDir != null) {
                if (c.projectsData.runtimeDir.path == null || c.projectsData.runtimeDir.path.isEmpty()) {
                    throw new ConfigurationValidationException("<runtime-dir><path> must be non-empty if <runtime-dir> is present.");
                }
                if (c.projectsData.runtimeDir.rotation != null) {
                    // enabled мог быть null, это допустимо по вашим правилам; timestamp-pattern валидируем, если указан
                    if (c.projectsData.runtimeDir.rotation.timestampPattern != null) {
                        try {
                            DateTimeFormatter.ofPattern(c.projectsData.runtimeDir.rotation.timestampPattern);
                        } catch (IllegalArgumentException iae) {
                            throw new ConfigurationValidationException(
                                    "rotation@timestamp-pattern is invalid: " + iae.getMessage(), iae);
                        }
                    }
                }
            }
        }
    }

}
