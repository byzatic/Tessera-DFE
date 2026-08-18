package io.github.byzatic.tessera.engine.application.runtime;

import io.github.byzatic.lib.configio.application.revision.ProjectRevision;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

/**
 * Creates an isolated object graph for a prepared project revision.
 */
public interface ProjectRuntimeFactory {

    /**
     * Creates but does not start a project runtime.
     *
     * @param revision loaded project revision retained by the caller
     * @return isolated runtime for the revision
     * @throws OperationIncompleteException when runtime resources cannot be prepared
     */
    ProjectRuntime create(ProjectRevision revision) throws OperationIncompleteException;
}
