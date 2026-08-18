package io.github.byzatic.tessera.engine.application.runtime;

/**
 * Receives fatal failures reported by a running project runtime.
 */
public interface ProjectRuntimeFailureListener {

    /**
     * Reports that a runtime can no longer execute its project revision.
     *
     * @param runtime failed runtime
     * @param failure fatal execution failure
     */
    void onFailure(ProjectRuntime runtime, Throwable failure);
}
