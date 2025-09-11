package io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository;

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
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common.ProjectConfigReader;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.SharedResourcesContainer;
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
    private final Map<ProjectLoaderTypes, ProjectLoaderInterface> projectLoaderTypedMap = new HashMap<>();

    private SharedResourcesContainer sharedResourcesContainer = null;
    private NodeContainer nodeContainer = null;
    private GlobalContainer globalContainer = null;

    public ProjectRepositoryImpl(String projectName) {
        this(projectName, false);
    }

    public ProjectRepositoryImpl(String projectName, Boolean loadNow) {
        this.projectName = projectName;
        if (loadNow) {
            try {
                load();
            } catch (OperationIncompleteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void addProjectLoader(ProjectLoaderTypes projectLoaderType, ProjectLoaderInterface projectLoader) {
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
        try {
            Map<String, ProjectLoaderTypes> projectLoaderTypesByProjectVersionMap = new HashMap<>();
            projectLoaderTypesByProjectVersionMap.put("v1.0.0-SingleRootStrictNestedNodeTree", ProjectLoaderTypes.PLV1);

            String projectVersion = ProjectConfigReader.readProjectConfigVersion(Configuration.PROJECTS_DIR.resolve(projectName).resolve("data").resolve("Project.json"));

            ProjectLoaderInterface projectLoader = null;
            if (projectLoaderTypesByProjectVersionMap.containsKey(projectVersion)) {
                if (projectLoaderTypedMap.containsKey(projectLoaderTypesByProjectVersionMap.get(projectVersion))) {
                    projectLoader = projectLoaderTypedMap.get(projectLoaderTypesByProjectVersionMap.get(projectVersion));
                } else {
                    String errMessage = "ProjectLoader for project version " + projectVersion + " was not found";
                    logger.error(errMessage);
                    throw new OperationIncompleteException(errMessage);
                }
            } else {
                String errMessage = "Unsupported project version: " + projectVersion;
                logger.error(errMessage);
                throw new OperationIncompleteException(errMessage);
            }

            sharedResourcesContainer = null;
            nodeContainer = null;
            globalContainer = null;

            // TODO: await load lock

            sharedResourcesContainer = projectLoader.getSharedResourcesContainer(projectName);
            nodeContainer = projectLoader.getNodeContainer(projectName);
            globalContainer = projectLoader.getGlobalContainer(projectName);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (OperationIncompleteException e) {
            throw new OperationIncompleteException(e);
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
}
