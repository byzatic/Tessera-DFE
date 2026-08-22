package io.github.byzatic.tessera.engine.application.commons.logging;

import org.junit.After;
import org.junit.Test;
import org.slf4j.MDC;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MdcContextScopeTest {

    @After
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void shouldRestorePreviousContextAfterNestedScope() throws Exception {
        MDC.put("identificationMessage", "type=Engine");
        MdcWorkflowRoutineContext routineContext = MdcWorkflowRoutineContext.newBuilder()
                .setNodeName("Node")
                .setNodeIndex("node-1")
                .setStageName("Stage")
                .setStageIndex("1")
                .setRoutineName("Routine")
                .setRoutineIndex("routine-1")
                .build();

        try (AutoCloseable ignored = routineContext.use()) {
            assertEquals(
                    "type=WorkflowRoutine Node::1:Stage|Routine:routine-1",
                    MDC.get("identificationMessage")
            );
        }

        assertEquals("type=Engine", MDC.get("identificationMessage"));
    }

    @Test
    public void shouldClearContextWhenNoPreviousContextExists() throws Exception {
        MdcWorkflowRoutineContext routineContext = MdcWorkflowRoutineContext.newBuilder()
                .setNodeName("Node")
                .setNodeIndex("node-1")
                .setStageName("Stage")
                .setStageIndex("1")
                .setRoutineName("Routine")
                .setRoutineIndex("routine-1")
                .build();

        try (AutoCloseable ignored = routineContext.use()) {
            assertEquals(
                    "type=WorkflowRoutine Node::1:Stage|Routine:routine-1",
                    MDC.get("identificationMessage")
            );
        }

        assertNull(MDC.get("identificationMessage"));
    }
}
