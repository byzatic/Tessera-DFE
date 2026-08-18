package io.github.byzatic.tessera.engine.application.runtime;

/**
 * Receives notifications when a stable and valid engine configuration is available.
 */
public interface ConfigurationChangeListener {

    /**
     * Requests application of the newly available engine configuration.
     */
    void onConfigurationChanged();
}
