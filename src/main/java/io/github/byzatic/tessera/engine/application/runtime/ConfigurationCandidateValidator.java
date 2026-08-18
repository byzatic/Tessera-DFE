package io.github.byzatic.tessera.engine.application.runtime;

/**
 * Validates a changed engine configuration before runtime replacement is requested.
 */
public interface ConfigurationCandidateValidator {

    /**
     * Validates the current candidate configuration.
     *
     * @throws Exception when the candidate must not be applied
     */
    void validate() throws Exception;
}
