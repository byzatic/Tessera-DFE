package io.github.byzatic.tessera.engine.infrastructure.configuration;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.runtime.ConfigurationCandidateValidator;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Validates the Tessera DFE XML configuration using the engine configuration loader.
 */
public final class XmlConfigurationCandidateValidator
        implements ConfigurationCandidateValidator {

    @Override
    public void validate() throws ConfigurationException {
        Configuration.validateCandidate();
    }
}
