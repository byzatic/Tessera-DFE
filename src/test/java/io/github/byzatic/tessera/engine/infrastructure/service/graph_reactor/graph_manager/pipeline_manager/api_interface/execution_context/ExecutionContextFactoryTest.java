package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.api_interface.execution_context;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesConsistencyItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.StagesDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.WorkersDescriptionItem;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.domain.repository.FullProjectRepository;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_management.GraphPathManagerInterface;
import io.github.byzatic.tessera.workflowroutine.execution_context.ExecutionContextInterface;
import org.junit.After;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionContextFactoryTest {

    @After
    public void clearMdc() {
        MDC.clear();
    }

    @Test
    public void shouldCreateWorkerSpecificContextForTheSameNode() throws Exception {
        GraphNodeRef nodeRef = GraphNodeRef.newBuilder().nodeUUID("node-uuid").build();
        FullProjectRepository repository = mock(FullProjectRepository.class);
        GraphPathManagerInterface graphPathManager = mock(GraphPathManagerInterface.class);
        when(repository.getNode(nodeRef)).thenReturn(NodeItem.newBuilder()
                .setId("node-id")
                .setName("pipeline_data")
                .setDownstream(Collections.emptyList())
                .build());
        when(repository.getNodeGlobal(nodeRef)).thenReturn(NodeGlobal.newBuilder()
                .storages(Collections.emptyList())
                .build());
        when(repository.getGlobal()).thenReturn(ProjectGlobal.newBuilder()
                .storages(Collections.emptyList())
                .services(Collections.emptyList())
                .build());
        when(graphPathManager.getRootPathsAsString(nodeRef, "."))
                .thenReturn(Collections.singletonList("pipeline_data"));
        ExecutionContextFactory factory = new ExecutionContextFactory(repository, graphPathManager);

        ExecutionContextInterface first = factory.getExecutionContext(
                nodeRef,
                Collections.singletonList(nodeRef),
                stage("TIME_DESYNC"),
                worker("GetDataWorkflowRoutine"),
                stagePosition("TIME_DESYNC", 1)
        );
        ExecutionContextInterface second = factory.getExecutionContext(
                nodeRef,
                Collections.singletonList(nodeRef),
                stage("ProcessingStatus"),
                worker("ProcessingStatusWorkflowRoutine"),
                stagePosition("ProcessingStatus", 2)
        );

        assertNotSame(first, second);
        assertEquals(
                "type=WorkflowRoutine pipeline_data::1:TIME_DESYNC|GetDataWorkflowRoutine:Null",
                identificationMessage(first)
        );
        assertEquals(
                "type=WorkflowRoutine pipeline_data::2:ProcessingStatus|ProcessingStatusWorkflowRoutine:Null",
                identificationMessage(second)
        );
    }

    private String identificationMessage(ExecutionContextInterface context) throws Exception {
        try (AutoCloseable ignored = context.getMdcContext().use()) {
            return MDC.get("identificationMessage");
        }
    }

    private StagesDescriptionItem stage(String name) {
        return StagesDescriptionItem.newBuilder().stageId(name).build();
    }

    private WorkersDescriptionItem worker(String name) {
        return WorkersDescriptionItem.newBuilder().name(name).build();
    }

    private StagesConsistencyItem stagePosition(String name, int position) {
        return StagesConsistencyItem.newBuilder().stageId(name).position(position).build();
    }
}
