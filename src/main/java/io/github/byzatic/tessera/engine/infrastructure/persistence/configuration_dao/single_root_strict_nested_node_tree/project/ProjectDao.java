package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node.Project;
import io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.dto.ConfigProject;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.ProjectDaoInterface;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.Map;

public class ProjectDao implements ProjectDaoInterface {
    private final static Logger logger = LoggerFactory.getLogger(ProjectDao.class);
    private final static Gson gson = new Gson();
    private final SupportNodesStructureCompressor supportNodesStructureCompressor = new SupportNodesStructureCompressor();

    public ProjectDao() {
    }

    @Override
    public Project load(Path projectFile) throws OperationIncompleteException {
        try {
            FileReader reader = new FileReader(projectFile.toFile());
            ConfigProject configProject = gson.fromJson(reader, ConfigProject.class);
            logger.debug("Loaded {} from {}", ConfigProject.class.getSimpleName(), projectFile);
            logger.trace("Loaded {} from {} -> {}", ConfigProject.class.getSimpleName(), projectFile, configProject);
            Map<GraphNodeRef, NodeItem> nodeMap = supportNodesStructureCompressor.uncompress(configProject);
            return Project.newBuilder()
                    .projectName(configProject.getProjectName())
                    .projectConfigVersion(configProject.getProjectConfigVersion())
                    .nodeMap(nodeMap)
                    .build();
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }
}
