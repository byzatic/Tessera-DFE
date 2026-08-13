package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

import io.github.byzatic.lib.configio.application.loader.ProjectLoaderInterface;
import io.github.byzatic.lib.configio.domain.exception.ProjectLoadingException;
import io.github.byzatic.lib.configio.domain.model.ProjectLoadResultDataObject;
import io.github.byzatic.lib.configio.infrastructure.factory.ProjectV1LoaderFactory;
import io.github.byzatic.tessera.engine.Configuration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRepositoryImpl implements ProjectRepository {
    private final static Logger logger = LoggerFactory.getLogger(ProjectRepositoryImpl.class);
    private final String projectName;
    private final Map<ProjectLoaderTypes, io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectLoaderInterface> projectLoaderTypedMap = new HashMap<>();
    private final ProjectLoaderInterface projectConfigurationLoader;
    private final ProjectConfigurationMapper projectConfigurationMapper;

    private SharedResourcesContainer sharedResourcesContainer = null;
    private NodeContainer nodeContainer = null;
    private GlobalContainer globalContainer = null;
    private ProjectLoadResultDataObject loadedProject = null;

    public ProjectRepositoryImpl(String projectName) {
        this(projectName, false);
    }

    public ProjectRepositoryImpl(String projectName, Boolean loadNow) {
        this.projectName = projectName;
        this.projectConfigurationLoader = ProjectV1LoaderFactory.create();
        this.projectConfigurationMapper = new ProjectConfigurationMapper();
        if (loadNow) {
            try {
                load();
            } catch (OperationIncompleteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void addProjectLoader(
            ProjectLoaderTypes projectLoaderType,
            io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectLoaderInterface projectLoader
    ) {
        projectLoaderTypedMap.put(projectLoaderType, projectLoader);
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

    @Override
    public void load() throws OperationIncompleteException {
        ProjectLoadResultDataObject newLoadedProject = null;
        try {
            newLoadedProject = projectConfigurationLoader.load(
                    Configuration.PROJECTS_DIR.resolve(projectName)
            );
            ProjectRepositoryStateDataObject newState =
                    projectConfigurationMapper.map(newLoadedProject);

            ProjectLoadResultDataObject previousLoadedProject = loadedProject;
            loadedProject = newLoadedProject;
            sharedResourcesContainer = newState.getSharedResourcesContainer();
            nodeContainer = newState.getNodeContainer();
            globalContainer = newState.getGlobalContainer();
            closeLoadedProject(previousLoadedProject);
        } catch (ProjectLoadingException e) {
            closeAfterFailure(newLoadedProject, e);
            throw new OperationIncompleteException("Cannot load project " + projectName, e);
        } catch (RuntimeException e) {
            closeAfterFailure(newLoadedProject, e);
            throw e;
        }
    }

    @Override
    public void reload() throws OperationIncompleteException {
        load();
    }

    @Override
    public @NotNull NodeToGNRContainer getNodeToGNRContainer() throws OperationIncompleteException {
        return new NodeToGNRContainer(nodeContainer.getNodeMap());
    }

    private void closeLoadedProject(ProjectLoadResultDataObject project) {
        if (project == null) {
            return;
        }
        try {
            project.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot close previously loaded project", e);
        }
    }

    private void closeAfterFailure(
            ProjectLoadResultDataObject project,
            Throwable failure
    ) {
        if (project == null) {
            return;
        }
        try {
            project.close();
        } catch (IOException e) {
            failure.addSuppressed(e);
        }
    }
}
