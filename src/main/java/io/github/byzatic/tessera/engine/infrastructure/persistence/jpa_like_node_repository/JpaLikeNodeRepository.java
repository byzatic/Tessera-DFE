package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_repository;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node.Project;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class JpaLikeNodeRepository implements JpaLikeNodeRepositoryInterface {
    private final static Logger logger = LoggerFactory.getLogger(JpaLikeNodeRepository.class);
    private final ProjectDaoInterface projectDao;
    private final SupportProjectValidation supportProjectValidation = new SupportProjectValidation();
    private final String projectName;
    private final Map<GraphNodeRef, NodeItem> nodeMap = new HashMap<>();
    private final List<GraphNodeRef> allGraphNodeRef = new LinkedList<>();


    public JpaLikeNodeRepository(@NotNull String projectName, @NotNull ProjectDaoInterface projectDao) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(projectName, new IllegalArgumentException("projectName must be NotNull"));
            this.projectName = projectName;
            ObjectsUtils.requireNonNull(projectDao, new IllegalArgumentException("configurationDao must be NotNull"));
            this.projectDao = projectDao;

            load(projectName);
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    private void load(String projectName) throws OperationIncompleteException {
        logger.debug("Loading jpa_like_project_global_repository");
        Path projectFilePath = Configuration.PROJECTS_DIR.resolve(projectName).resolve("data").resolve("Project.json");
        Project project = projectDao.load(projectFilePath);
        logger.debug("Loaded jpa_like_project_global_repository {}", project);
        supportProjectValidation.validate(project);
        nodeMap.putAll(project.getNodeMap());
        logger.debug("Loading jpa_like_project_global_repository complete; nodeMap size is {}", nodeMap.size());
    }

    @Override
    public void reload() throws OperationIncompleteException {
        try {
            nodeMap.clear();
            allGraphNodeRef.clear();
            load(projectName);
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public NodeItem getNode(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        logger.debug("Requested {} by {} -> {}", NodeItem.class.getSimpleName(), GraphNodeRef.class.getSimpleName(), graphNodeRef);
        NodeItem result = null;
        if (nodeMap.containsKey(graphNodeRef)) {
            result = nodeMap.get(graphNodeRef);
        } else {
            String errMessage = "Repository " + JpaLikeNodeRepository.class.getSimpleName() + " not contains object " + NodeItem.class.getSimpleName() + " requested by " + graphNodeRef;
            logger.error(errMessage);
            logger.error("Repository -> {}", nodeMap);
            throw new OperationIncompleteException(errMessage);
        }
        logger.debug("Returns requested {}", NodeItem.class.getSimpleName());
        logger.trace("Returns requested {} -> {}", NodeItem.class.getSimpleName(), result);
        return result;
    }

    @Override
    public List<GraphNodeRef> getAllGraphNodeRef() throws OperationIncompleteException {
        logger.debug("Requested List of all {}", GraphNodeRef.class.getSimpleName());
        if (allGraphNodeRef.isEmpty()) {
            for (Map.Entry<GraphNodeRef, NodeItem> nodeMapEntry : nodeMap.entrySet()) {
                allGraphNodeRef.add(nodeMapEntry.getKey());
                logger.debug("Created List of all {}", GraphNodeRef.class.getSimpleName());
            }
        }
        logger.debug("Returns requested List of all {} of size {}", GraphNodeRef.class.getSimpleName(), allGraphNodeRef.size());
        logger.trace("Returns requested List of all {} -> {}", GraphNodeRef.class.getSimpleName(), allGraphNodeRef);
        return allGraphNodeRef;
    }
}
