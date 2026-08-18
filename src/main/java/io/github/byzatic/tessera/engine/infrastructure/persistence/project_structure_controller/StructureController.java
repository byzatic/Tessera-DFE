package io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_controller;

import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.infrastructure.persistence.project_repository.common.NodeToGNRContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;

public class StructureController implements StructureControllerInterface {
    private final static Logger logger = LoggerFactory.getLogger(StructureController.class);
    private final Path projectsDirectory;

    /**
     * Creates a controller for an isolated project revision directory.
     *
     * @param projectDirectory extracted project root
     */
    public StructureController(Path projectDirectory) {
        this.projectsDirectory = Objects.requireNonNull(
                projectDirectory,
                "projectDirectory"
        ).toAbsolutePath().normalize();
    }

    @Override
    public NodeStructure getNodeStructure(GraphNodeRef graphNodeRef, NodeToGNRContainer nodeToGNRContainer) {
        try {
            NodeItem node = nodeToGNRContainer.getNode(graphNodeRef);
            String nodeFolderName = node.getId();
            if (node.getId().equals("#NAMED")) {
                nodeFolderName = node.getName();
            } else {
                nodeFolderName = node.getId() + "-" + node.getName();
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
}
