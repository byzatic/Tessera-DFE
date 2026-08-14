package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

public class ProjectRepositoryImplIntegrationTest {

    @Test
    public void shouldPreserveRepositoryContractWhenLoadingThroughConfigIoLibrary()
            throws Exception {
        Path projectDirectory = Configuration.PROJECTS_DIR.resolve("MyAwesomeProject");
        assumeTrue(
                "Integration project is not available: " + projectDirectory,
                Files.isDirectory(projectDirectory)
        );

        ProjectRepositoryImpl repository = new ProjectRepositoryImpl("MyAwesomeProject");

        repository.load();

        List<GraphNodeRef> nodeReferences = repository.listGraphNodeRef();
        assertEquals(55, nodeReferences.size());
        assertEquals(1, repository.getGlobal().getStorages().size());
        assertEquals(1, repository.getGlobal().getServices().size());

        GraphNodeRef firstNodeReference = nodeReferences.get(0);
        assertNotNull(repository.getNode(firstNodeReference));
        assertEquals(3, repository.getNodeGlobal(firstNodeReference).getStorages().size());
        assertEquals(3, repository.getPipeline(firstNodeReference)
                .getStagesConsistency().size());
        assertNotNull(repository.getSharedResourcesClassLoader());
    }
}
