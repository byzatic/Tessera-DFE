package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface;

import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context.ExecutionContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BuilderValidationTest {

    @Test
    public void executionContextShouldRejectMissingPipelineDescription() {
        assertIllegalArgument(
                "pipelineDescription must be not null",
                () -> ExecutionContext.newBuilder().build()
        );
    }

    @Test
    public void workflowRoutineApiShouldRejectMissingStorageApi() {
        assertIllegalArgument(
                "Can't create MCg3WorkflowRoutineApi with null StorageApiInterface",
                () -> MCg3WorkflowRoutineApi.newBuilder().build()
        );
    }

    private void assertIllegalArgument(String expectedMessage, Runnable action) {
        try {
            action.run();
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
            assertEquals(expectedMessage, exception.getMessage());
        }
    }
}
