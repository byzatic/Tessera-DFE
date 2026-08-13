package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProjectRepositoryImplIntegrationTest {

    @Test
    public void shouldPreserveRepositoryContractWhenLoadingThroughConfigIoLibrary()
            throws Exception {
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
