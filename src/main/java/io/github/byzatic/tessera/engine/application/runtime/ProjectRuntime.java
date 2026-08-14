package io.github.byzatic.tessera.engine.application.runtime;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

import java.time.Duration;

/**
 * Owns all executable resources associated with one immutable project revision.
 */
public interface ProjectRuntime extends AutoCloseable {

    /**
     * Starts services and graph scheduling for the revision.
     *
     * @param startupTimeout maximum time allowed for reaching the running state
     * @throws OperationIncompleteException when the runtime cannot be started
     */
    void start(Duration startupTimeout) throws OperationIncompleteException;

    /**
     * Stops the runtime and releases all revision-specific executable resources.
     *
     * @param shutdownTimeout maximum time allowed for cooperative shutdown
     */
    void stop(Duration shutdownTimeout);

    /**
     * Returns the identifier of the project revision served by this runtime.
     *
     * @return revision identifier
     */
    String getRevisionId();

    /**
     * Performs an idempotent stop using the runtime default timeout.
     */
    @Override
    void close();
}
