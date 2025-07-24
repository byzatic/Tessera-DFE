package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

public interface PipelineManagerInterface {
    void runPipeline() throws OperationIncompleteException;
}
