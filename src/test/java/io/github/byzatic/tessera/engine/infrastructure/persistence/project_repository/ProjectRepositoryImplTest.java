package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.tessera.lib.configio.unified.model.NodeConfiguration;
import io.github.byzatic.tessera.lib.configio.unified.model.NodeId;
import io.github.byzatic.tessera.lib.configio.unified.model.Pipeline;
import io.github.byzatic.tessera.lib.configio.unified.model.PipelineStage;
import io.github.byzatic.tessera.lib.configio.unified.model.ProjectConfiguration;
import io.github.byzatic.tessera.lib.configio.unified.model.ProjectNode;
import io.github.byzatic.tessera.lib.configio.unified.model.ServiceDefinition;
import io.github.byzatic.tessera.lib.configio.unified.model.TesseraProject;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class ProjectRepositoryImplTest {

    private static final String NODE_UUID = "node-uuid";

    @Test
    public void shouldMapUnifiedProjectToRepositoryState() throws Exception {
        NodeId nodeId = NodeId.newBuilder().value(NODE_UUID).build();
        Pipeline pipeline = Pipeline.newBuilder()
                .stages(List.of(
                        PipelineStage.newBuilder()
                                .id("stage-id")
                                .position(1)
                                .workers(List.of())
                                .build()
                ))
                .build();
        ProjectNode node = ProjectNode.newBuilder()
                .nodeId(nodeId)
                .id("node-id")
                .name("Test node")
                .description("Unified repository fixture")
                .downstream(List.of())
                .configuration(NodeConfiguration.newBuilder().build())
                .pipeline(pipeline)
                .build();
        TesseraProject project = TesseraProject.newBuilder()
                .formatVersion("v1")
                .name("test-project")
                .configuration(ProjectConfiguration.newBuilder().build())
                .nodes(Map.of(nodeId, node))
                .build();

        ProjectRepositoryImpl repository = new ProjectRepositoryImpl(project);

        List<GraphNodeRef> nodeReferences = repository.listGraphNodeRef();
        assertEquals(1, nodeReferences.size());
        GraphNodeRef nodeReference = nodeReferences.get(0);
        assertEquals(NODE_UUID, nodeReference.getNodeUUID());
        assertNotNull(repository.getNode(nodeReference));
        assertEquals(1, repository.getPipeline(nodeReference)
                .getStagesConsistency().size());
    }

    @Test
    public void shouldRejectDuplicateServiceIds() {
        ServiceDefinition first = ServiceDefinition.newBuilder()
                .id("duplicate")
                .description("first")
                .build();
        ServiceDefinition second = ServiceDefinition.newBuilder()
                .id("duplicate")
                .description("second")
                .build();
        TesseraProject project = TesseraProject.newBuilder()
                .formatVersion("v1")
                .name("duplicate-services")
                .configuration(ProjectConfiguration.newBuilder()
                        .services(List.of(first, second))
                        .build())
                .nodes(Map.of())
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRepositoryImpl(project)
        );

        assertEquals("Duplicate service id: duplicate", exception.getMessage());
    }
}
