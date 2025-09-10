package io.github.byzatic.tessera.engine.infrastructure.persistence.trash.resource_manager.common_v1_hierarchical;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_global_dao.NodeGlobalDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.node_pipeline_dao.PipelineDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.ProjectDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_global.ProjectGlobalDao;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureController;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project_structure_controller.StructureControllerInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.*;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.jpa_like_node_global_repository.JpaLikeNodeGlobalRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.NodeGlobalDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.jpa_like_node_repository.JpaLikeNodeRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.ProjectDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.jpa_like_pipeline_repository.JpaLikePipelineRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.PipelineDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.jpa_like_project_global_repository.JpaLikeProjectGlobalRepository;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.ProjectGlobalDaoInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.shared_resources_manager.SharedResourcesRepository;

public class ResourcesContext {
    private JpaLikeProjectGlobalRepositoryInterface projectGlobalRepository = null;
    private JpaLikeNodeGlobalRepositoryInterface nodeGlobalRepository = null;
    private JpaLikeNodeRepositoryInterface nodeRepository = null;
    private JpaLikePipelineRepositoryInterface pipelineRepository = null;
    private SharedResourcesRepositoryInterface sharedResourcesManager = null;
    private ProjectDaoInterface projectDao = null;
    private NodeGlobalDaoInterface nodeGlobalDaoInterface = null;
    private PipelineDaoInterface pipelineDao = null;
    private ProjectGlobalDaoInterface projectGlobalDao = null;
    private StructureControllerInterface structureManager = null;
    private String resourcesContextProjectName = null;

    static {
        Configuration.MDC_ENGINE_CONTEXT.apply();
    }

    public ResourcesContext(String resourcesContextProjectName) {
        this.resourcesContextProjectName = resourcesContextProjectName;
    }

    public ProjectDaoInterface getProjectDao() {
        if (projectDao == null) {
            projectDao = new ProjectDao();
        }
        return projectDao;
    }

    public ProjectGlobalDaoInterface getProjectGlobalDao() {
        try {
            if (projectGlobalDao == null) {
                projectGlobalDao = new ProjectGlobalDao();
            }
            return projectGlobalDao;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public NodeGlobalDaoInterface getNodeGlobalDao() {
        if (nodeGlobalDaoInterface == null) {
            nodeGlobalDaoInterface = new NodeGlobalDao(getNodeRepository(), getStructureManager());
        }
        return nodeGlobalDaoInterface;
    }

    public PipelineDaoInterface getPipelineDao() {
        try {
            if (pipelineDao == null) {
                pipelineDao = new PipelineDao(
                        getNodeRepository(),
                        getStructureManager()
                );
            }
            return pipelineDao;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public StructureControllerInterface getStructureManager() {
        try {
            if (structureManager == null) {
                structureManager = new StructureController(
                        getNodeRepository(),
                        resourcesContextProjectName
                );
            }
            return structureManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JpaLikePipelineRepositoryInterface getPipelineRepository() {
        try {
            if (pipelineRepository == null) {
                pipelineRepository = new JpaLikePipelineRepository(
                        resourcesContextProjectName,
                        getPipelineDao(),
                        100L
                );
            }
            return pipelineRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JpaLikeNodeRepositoryInterface getNodeRepository() {
        try {
            if (nodeRepository == null) {
                nodeRepository = new JpaLikeNodeRepository(
                        resourcesContextProjectName,
                        getProjectDao(),
                        100L
                );
            }
            return nodeRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SharedResourcesRepositoryInterface getSharedResourcesManager() {
        try {
            if (sharedResourcesManager == null) {
                sharedResourcesManager = new SharedResourcesRepository(
                        resourcesContextProjectName
                );
            }
            return sharedResourcesManager;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JpaLikeNodeGlobalRepositoryInterface getNodeGlobalRepository() {
        try {
            if (nodeGlobalRepository == null) {
                nodeGlobalRepository = new JpaLikeNodeGlobalRepository(
                        resourcesContextProjectName,
                        getNodeGlobalDao(),
                        100L
                );
            }
            return nodeGlobalRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JpaLikeProjectGlobalRepositoryInterface getProjectGlobalRepository() {
        try {
            if (projectGlobalRepository == null) {
                projectGlobalRepository = new JpaLikeProjectGlobalRepository(
                        resourcesContextProjectName,
                        getProjectGlobalDao(),
                        100L
                );
            }
            return projectGlobalRepository;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
