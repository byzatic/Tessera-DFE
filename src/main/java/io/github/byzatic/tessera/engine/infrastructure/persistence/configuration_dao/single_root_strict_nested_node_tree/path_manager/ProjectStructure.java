package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.path_manager;

import java.nio.file.Path;
import java.util.Objects;

public class ProjectStructure {
    private Path projectFolder;
    private Path projectConfigurationFilesFolder;

    public ProjectStructure() {
    }

    private ProjectStructure(Builder builder) {
        projectFolder = builder.projectFolder;
        projectConfigurationFilesFolder = builder.projectConfigurationFilesFolder;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ProjectStructure copy) {
        Builder builder = new Builder();
        builder.projectFolder = copy.getProjectFolder();
        builder.projectConfigurationFilesFolder = copy.getProjectConfigurationFilesFolder();
        return builder;
    }

    public Path getProjectFolder() {
        return projectFolder;
    }

    public Path getProjectConfigurationFilesFolder() {
        return projectConfigurationFilesFolder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectStructure that = (ProjectStructure) o;
        return Objects.equals(projectFolder, that.projectFolder) && Objects.equals(projectConfigurationFilesFolder, that.projectConfigurationFilesFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectFolder, projectConfigurationFilesFolder);
    }

    @Override
    public String toString() {
        return "NodeStructure{" +
                "projectFolder=" + projectFolder +
                ", projectConfigurationFilesFolder=" + projectConfigurationFilesFolder +
                '}';
    }

    /**
     * {@code NodeStructure} builder static inner class.
     */
    public static final class Builder {
        private Path projectFolder;
        private Path projectConfigurationFilesFolder;

        private Builder() {
        }

        /**
         * Sets the {@code projectFolder} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param projectFolder the {@code projectFolder} to set
         * @return a reference to this Builder
         */
        public Builder setProjectFolder(Path projectFolder) {
            this.projectFolder = projectFolder;
            return this;
        }

        /**
         * Sets the {@code projectConfigurationFilesFolder} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param projectConfigurationFilesFolder the {@code projectConfigurationFilesFolder} to set
         * @return a reference to this Builder
         */
        public Builder setProjectConfigurationFilesFolder(Path projectConfigurationFilesFolder) {
            this.projectConfigurationFilesFolder = projectConfigurationFilesFolder;
            return this;
        }

        /**
         * Returns a {@code NodeStructure} built from the parameters previously set.
         *
         * @return a {@code NodeStructure} built with parameters of this {@code NodeStructure.Builder}
         */
        public ProjectStructure build() {
            return new ProjectStructure(this);
        }
    }
}
