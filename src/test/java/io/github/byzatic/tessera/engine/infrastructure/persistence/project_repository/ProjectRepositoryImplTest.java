package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.lib.configio.domain.model.GraphNodeReferenceDataObject;
import io.github.byzatic.lib.configio.domain.model.NodeContainerDataObject;
import io.github.byzatic.lib.configio.domain.model.NodeDataObject;
import io.github.byzatic.lib.configio.domain.model.NodeGlobalDataObject;
import io.github.byzatic.lib.configio.domain.model.PipelineDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectGlobalDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectLoadResultDataObject;
import io.github.byzatic.lib.configio.domain.model.ProjectStructureDataObject;
import io.github.byzatic.lib.configio.domain.model.SharedResourcesContainerDataObject;
import io.github.byzatic.lib.configio.domain.model.StageConsistencyDataObject;
import io.github.byzatic.lib.configio.unified.model.NodeConfiguration;
import io.github.byzatic.lib.configio.unified.model.NodeId;
import io.github.byzatic.lib.configio.unified.model.Pipeline;
import io.github.byzatic.lib.configio.unified.model.PipelineStage;
import io.github.byzatic.lib.configio.unified.model.ProjectConfiguration;
import io.github.byzatic.lib.configio.unified.model.ProjectNode;
import io.github.byzatic.lib.configio.unified.model.TesseraProject;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ProjectRepositoryImplTest {

    private static final String NODE_UUID = "node-uuid";

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldMapLoadedProjectToRepositoryState() throws Exception {
        Path projectDirectory = temporaryFolder.newFolder("project").toPath();

        try (ProjectLoadResultDataObject loadedProject = createLoadedProject(projectDirectory)) {
            ProjectRepositoryImpl repository = new ProjectRepositoryImpl(loadedProject);

            List<GraphNodeRef> nodeReferences = repository.listGraphNodeRef();
            assertEquals(1, nodeReferences.size());
            assertEquals(0, repository.getGlobal().getStorages().size());
            assertEquals(0, repository.getGlobal().getServices().size());

            GraphNodeRef nodeReference = nodeReferences.get(0);
            assertEquals(NODE_UUID, nodeReference.getNodeUUID());
            assertNotNull(repository.getNode(nodeReference));
            assertEquals(0, repository.getNodeGlobal(nodeReference).getStorages().size());
            assertEquals(1, repository.getPipeline(nodeReference)
                    .getStagesConsistency().size());
            assertNotNull(repository.getSharedResourcesClassLoader());
        }
    }

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
        assertNull(repository.getSharedResourcesClassLoader());
    }

    private ProjectLoadResultDataObject createLoadedProject(Path projectDirectory) {
        GraphNodeReferenceDataObject nodeReference =
                new GraphNodeReferenceDataObject(NODE_UUID);
        Map<GraphNodeReferenceDataObject, NodeDataObject> nodes =
                new LinkedHashMap<GraphNodeReferenceDataObject, NodeDataObject>();
        nodes.put(nodeReference, new NodeDataObject(
                NODE_UUID,
                "node-id",
                "Test node",
                "Self-contained repository fixture",
                new ArrayList<GraphNodeReferenceDataObject>()
        ));

        ProjectStructureDataObject structure = new ProjectStructureDataObject(
                new ProjectDataObject("v1", "test-project"),
                nodes
        );
        Map<GraphNodeReferenceDataObject, NodeGlobalDataObject> nodeGlobals =
                new LinkedHashMap<GraphNodeReferenceDataObject, NodeGlobalDataObject>();
        nodeGlobals.put(nodeReference, new NodeGlobalDataObject(null));
        Map<GraphNodeReferenceDataObject, PipelineDataObject> pipelines =
                new LinkedHashMap<GraphNodeReferenceDataObject, PipelineDataObject>();
        List<StageConsistencyDataObject> stages =
                new ArrayList<StageConsistencyDataObject>();
        stages.add(new StageConsistencyDataObject("stage-id", 1));
        pipelines.put(nodeReference, new PipelineDataObject(stages, null));

        List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        classLoaders.add(getClass().getClassLoader());
        SharedResourcesContainerDataObject resources =
                new SharedResourcesContainerDataObject(
                        classLoaders,
                        new ArrayList<URLClassLoader>()
                );
        return new ProjectLoadResultDataObject(
                projectDirectory,
                new ProjectGlobalDataObject(null, null),
                new NodeContainerDataObject(structure, nodeGlobals, pipelines),
                resources
        );
    }
}
