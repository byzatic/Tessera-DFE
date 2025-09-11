package io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader;

import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node.Project;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.common.NodeToGNRContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.ProjectLoaderInterface;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.GlobalContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.NodeContainer;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.dto.SharedResourcesContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ProjectV1Loader implements ProjectLoaderInterface {
    private final static Logger logger = LoggerFactory.getLogger(ProjectV1Loader.class);
    private final NodeGlobalDaoInterface nodeGlobalDao;
    private final PipelineDaoInterface pipelineDao;
    private final ProjectDaoInterface projectDao;
    private final ProjectGlobalDaoInterface projectGlobalDao;
    private final SharedResourcesDAOInterface sharedResourcesDAO;

    public ProjectV1Loader(NodeGlobalDaoInterface nodeGlobalDao, PipelineDaoInterface pipelineDao, ProjectDaoInterface projectDao, ProjectGlobalDaoInterface projectGlobalDao, SharedResourcesDAOInterface sharedResourcesDAO) {
        this.nodeGlobalDao = nodeGlobalDao;
        this.pipelineDao = pipelineDao;
        this.projectDao = projectDao;
        this.projectGlobalDao = projectGlobalDao;
        this.sharedResourcesDAO = sharedResourcesDAO;
    }

    @Override
    public @NotNull GlobalContainer getGlobalContainer(@NotNull String projectName) {
        try {
            GlobalContainer globalContainer = new GlobalContainer(projectGlobalDao.load(projectName));
            return globalContainer;
        } catch (OperationIncompleteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull NodeContainer getNodeContainer(@NotNull String projectName) {
        try {
            NodeContainer nodeContainer = null;

            Map<GraphNodeRef, NodeItem>  graphNodeRefNodeItemMap = projectDao.load().getNodeMap();
            NodeToGNRContainer nodeToGNRContainer = new NodeToGNRContainer(graphNodeRefNodeItemMap);
            Map<GraphNodeRef, NodeGlobal> graphNodeRefNodeGlobalMap = nodeGlobalDao.load(projectName, nodeToGNRContainer);
            Map<GraphNodeRef, NodePipeline> graphNodeRefNodePipelineMap = pipelineDao.load(projectName, nodeToGNRContainer);

            nodeContainer = new NodeContainer(
                    graphNodeRefNodeItemMap,
                    graphNodeRefNodeGlobalMap,
                    graphNodeRefNodePipelineMap
            );
            return nodeContainer;
        } catch (OperationIncompleteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull SharedResourcesContainer getSharedResourcesContainer(@NotNull String projectName) {
        SharedResourcesContainer sharedResourcesContainer = new SharedResourcesContainer(sharedResourcesDAO.loadSharedResources(projectName));
        return sharedResourcesContainer;
    }
}
