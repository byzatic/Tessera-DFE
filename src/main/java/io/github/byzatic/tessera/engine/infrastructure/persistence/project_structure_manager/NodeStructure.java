package io.github.byzatic.tessera.engine.infrastructure.persistence.project_structure_manager;

import java.nio.file.Path;
import java.util.Objects;

public class NodeStructure {
    private Path nodeFolder;
    private Path nodeConfigurationFilesFolder;

    public NodeStructure() {
    }

    private NodeStructure(Builder builder) {
        nodeFolder = builder.nodeFolder;
        nodeConfigurationFilesFolder = builder.nodeConfigurationFilesFolder;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(NodeStructure copy) {
        Builder builder = new Builder();
        builder.nodeFolder = copy.getNodeFolder();
        builder.nodeConfigurationFilesFolder = copy.getNodeConfigurationFilesFolder();
        return builder;
    }

    public Path getNodeFolder() {
        return nodeFolder;
    }

    public Path getNodeConfigurationFilesFolder() {
        return nodeConfigurationFilesFolder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeStructure that = (NodeStructure) o;
        return Objects.equals(nodeFolder, that.nodeFolder) && Objects.equals(nodeConfigurationFilesFolder, that.nodeConfigurationFilesFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeFolder, nodeConfigurationFilesFolder);
    }

    @Override
    public String toString() {
        return "NodeStructure{" +
                "nodeFolder=" + nodeFolder +
                ", nodeConfigurationFilesFolder=" + nodeConfigurationFilesFolder +
                '}';
    }

    /**
     * {@code NodeStructure} builder static inner class.
     */
    public static final class Builder {
        private Path nodeFolder;
        private Path nodeConfigurationFilesFolder;

        private Builder() {
        }

        /**
         * Sets the {@code nodeFolder} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param nodeFolder the {@code nodeFolder} to set
         * @return a reference to this Builder
         */
        public Builder setNodeFolder(Path nodeFolder) {
            this.nodeFolder = nodeFolder;
            return this;
        }

        /**
         * Sets the {@code nodeConfigurationFilesFolder} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param nodeConfigurationFilesFolder the {@code nodeConfigurationFilesFolder} to set
         * @return a reference to this Builder
         */
        public Builder setNodeConfigurationFilesFolder(Path nodeConfigurationFilesFolder) {
            this.nodeConfigurationFilesFolder = nodeConfigurationFilesFolder;
            return this;
        }

        /**
         * Returns a {@code NodeStructure} built from the parameters previously set.
         *
         * @return a {@code NodeStructure} built with parameters of this {@code NodeStructure.Builder}
         */
        public NodeStructure build() {
            return new NodeStructure(this);
        }
    }
}
