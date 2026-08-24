package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.tessera.lib.configio.unified.model.TesseraProject;
import io.github.byzatic.tessera.lib.configio.unified.model.ServiceDefinition;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.domain.repository.ProjectRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common.NodeToGNRContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper.ProjectConfigurationMapper;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper.ProjectRepositoryStateDataObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProjectRepositoryImpl implements ProjectRepository {
    private final ProjectConfigurationMapper projectConfigurationMapper;

    private NodeContainer nodeContainer;
    private GlobalContainer globalContainer;

    /**
     * Creates repository state from the detached unified project model.
     *
     * <p>Runtime resources and their class loaders remain owned by the project runtime
     * session and are not retained by this repository.</p>
     *
     * @param project immutable project configuration
     */
    public ProjectRepositoryImpl(TesseraProject project) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        validateUniqueServiceIds(project);
        this.projectConfigurationMapper = new ProjectConfigurationMapper();
        applyState(project);
    }

    private static void validateUniqueServiceIds(TesseraProject project) {
        Set<String> serviceIds = new HashSet<String>();
        for (ServiceDefinition service : project.getConfiguration().getServices()) {
            if (!serviceIds.add(service.getId())) {
                throw new IllegalArgumentException("Duplicate service id: " + service.getId());
            }
        }
    }

    @Override
    public @NotNull List<GraphNodeRef> listGraphNodeRef() throws OperationIncompleteException {
        return nodeContainer.listGraphNodeRef();
    }

    @Override
    public @NotNull NodeItem getNode(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        return nodeContainer.getNode(graphNodeRef);
    }

    @Override
    public @NotNull NodeGlobal getNodeGlobal(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        return nodeContainer.getNodeGlobal(graphNodeRef);
    }

    @Override
    public @NotNull Boolean isNodeStorageDeclaration(@NotNull GraphNodeRef graphNodeRef, @NotNull String storageName) {
        boolean isExists = false;
        for (StoragesItem storageItem : nodeContainer.getNodeGlobal(graphNodeRef).getStorages()) {
            if (storageItem.getIdName().equals(storageName)) {
                isExists = true;
                break;
            }
        }
        return isExists;
    }

    @Override
    public @NotNull NodePipeline getPipeline(@NotNull GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        return nodeContainer.getNodePipeline(graphNodeRef);
    }

    @Override
    public @NotNull ProjectGlobal getGlobal() {
        return globalContainer.getProjectGlobal();
    }

    @Override
    public @NotNull Boolean isGlobalStorageDeclaration(@NotNull String storageName) {
        boolean isExists = false;
        for (io.github.byzatic.tessera.engine.domain.model.project.StoragesItem storageItem : globalContainer.getProjectGlobal().getStorages()) {
            if (storageItem.getIdName().equals(storageName)) {
                isExists = true;
                break;
            }
        }
        return isExists;
    }

    private void applyState(TesseraProject source) {
        ProjectRepositoryStateDataObject state = projectConfigurationMapper.map(source);
        nodeContainer = state.getNodeContainer();
        globalContainer = state.getGlobalContainer();
    }

    @Override
    public @NotNull NodeToGNRContainer getNodeToGNRContainer() throws OperationIncompleteException {
        return new NodeToGNRContainer(nodeContainer.getNodeMap());
    }

}
