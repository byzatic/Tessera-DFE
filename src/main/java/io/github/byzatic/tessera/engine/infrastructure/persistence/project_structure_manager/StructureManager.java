package io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeRepositoryInterface;

import java.nio.file.Path;

public class StructureManager implements StructureManagerInterface {
    private final static Logger logger= LoggerFactory.getLogger(StructureManager.class);
    private final JpaLikeNodeRepositoryInterface jpaLikeNodeRepository;
    private final Path projectsDirectory;

    public StructureManager(JpaLikeNodeRepositoryInterface jpaLikeNodeRepository, String projectName) {
        this.jpaLikeNodeRepository = jpaLikeNodeRepository;

        //TODO: check if exists
        this.projectsDirectory = Configuration.PROJECTS_DIR.resolve(projectName);
    }

    @Override
    public NodeStructure getNodeStructure(GraphNodeRef graphNodeRef) {
        try {
            NodeItem node= this.jpaLikeNodeRepository.getNode(graphNodeRef);
            String nodeFolderName = node.getId();
            if (node.getId().equals("#NAMED")) {
                nodeFolderName= node.getName();
            } else {
                nodeFolderName= node.getId() + "-" + node.getName();
            }
            Path nodePath = projectsDirectory.resolve("data").resolve("nodes").resolve(nodeFolderName);
            Path nodeConfigurationFilesPath = nodePath.resolve("configuration_files");
            //TODO: check if exists
            return NodeStructure.newBuilder()
                    .setNodeFolder(nodePath)
                    .setNodeConfigurationFilesFolder(nodeConfigurationFilesPath)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public ProjectStructure getProjectStructure() {
        try {
            Path projectConfigurationFilesPath = this.projectsDirectory.resolve("data").resolve("configuration_files");
            //TODO: check if exists
            return ProjectStructure.newBuilder()
                    .setProjectFolder(this.projectsDirectory)
                    .setProjectConfigurationFilesFolder(projectConfigurationFilesPath)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void load() {
        //...
    }

    @Override
    public void reload() {
        //...
    }
}
