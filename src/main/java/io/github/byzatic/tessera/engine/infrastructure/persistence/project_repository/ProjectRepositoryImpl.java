package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.lib.configio.domain.model.ProjectLoadResultDataObject;
import io.github.byzatic.lib.configio.unified.model.TesseraProject;
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
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.SharedResourcesContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper.ProjectConfigurationMapper;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.mapper.ProjectRepositoryStateDataObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class ProjectRepositoryImpl implements ProjectRepository {
    private final ProjectConfigurationMapper projectConfigurationMapper;

    private SharedResourcesContainer sharedResourcesContainer;
    private NodeContainer nodeContainer;
    private GlobalContainer globalContainer;

    /**
     * Creates repository state from a revision loaded by config-io.
     *
     * <p>The caller retains ownership of {@code loadedProject} and must keep it open while
     * this repository is in use.</p>
     *
     * @param loadedProject loaded immutable project revision
     */
    public ProjectRepositoryImpl(ProjectLoadResultDataObject loadedProject) {
        if (loadedProject == null) {
            throw new IllegalArgumentException("loadedProject must not be null");
        }
        this.projectConfigurationMapper = new ProjectConfigurationMapper();
        applyState(loadedProject);
    }

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
        this.projectConfigurationMapper = new ProjectConfigurationMapper();
        applyState(project);
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

    @Override
    public @Nullable ClassLoader getSharedResourcesClassLoader() {
        return sharedResourcesContainer.getSharedResourcesClassLoader();
    }

    private void applyState(ProjectLoadResultDataObject source) {
        ProjectRepositoryStateDataObject state = projectConfigurationMapper.map(source);
        sharedResourcesContainer = state.getSharedResourcesContainer();
        nodeContainer = state.getNodeContainer();
        globalContainer = state.getGlobalContainer();
    }

    private void applyState(TesseraProject source) {
        ProjectRepositoryStateDataObject state = projectConfigurationMapper.map(source);
        sharedResourcesContainer = state.getSharedResourcesContainer();
        nodeContainer = state.getNodeContainer();
        globalContainer = state.getGlobalContainer();
    }

    @Override
    public @NotNull NodeToGNRContainer getNodeToGNRContainer() throws OperationIncompleteException {
        return new NodeToGNRContainer(nodeContainer.getNodeMap());
    }

}
