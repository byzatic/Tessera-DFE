package io.github.byzatic.tessera.engine.infrastructure.persistence.configuration_dao.single_root_strict_nested_node_tree.project.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class ConfigProject {

    @SerializedName("project_config_version")
    private String projectConfigVersion;

    @SerializedName("project_name")
    private String projectName;

    @SerializedName("structure")
    private ConfigNodeItem nodeItem;

    public ConfigProject() {
    }

    private ConfigProject(Builder builder) {
        projectConfigVersion = builder.projectConfigVersion;
        projectName = builder.projectName;
        nodeItem = builder.nodeItem;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ConfigProject copy) {
        Builder builder = new Builder();
        builder.projectConfigVersion = copy.getProjectConfigVersion();
        builder.projectName = copy.getProjectName();
        builder.nodeItem = copy.getStructure();
        return builder;
    }

    public String getProjectConfigVersion() {
        return projectConfigVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public ConfigNodeItem getStructure() {
        return nodeItem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigProject configProject = (ConfigProject) o;
        return Objects.equals(projectConfigVersion, configProject.projectConfigVersion) && Objects.equals(projectName, configProject.projectName) && Objects.equals(nodeItem, configProject.nodeItem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectConfigVersion, projectName, nodeItem);
    }

    @Override
    public String toString() {
        return "ConfigProject{" +
                "projectConfigVersion='" + projectConfigVersion + '\'' +
                ", projectName='" + projectName + '\'' +
                ", nodeItem=" + nodeItem +
                '}';
    }

    /**
     * {@code ConfigProject} builder static inner class.
     */
    public static final class Builder {
        private String projectConfigVersion;
        private String projectName;
        private ConfigNodeItem nodeItem;

        private Builder() {
        }

        /**
         * Sets the {@code projectConfigVersion} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code projectConfigVersion} to set
         * @return a reference to this Builder
         */
        public Builder projectConfigVersion(String val) {
            projectConfigVersion = val;
            return this;
        }

        /**
         * Sets the {@code projectName} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code projectName} to set
         * @return a reference to this Builder
         */
        public Builder projectName(String val) {
            projectName = val;
            return this;
        }

        /**
         * Sets the {@code nodeItem} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code nodeItem} to set
         * @return a reference to this Builder
         */
        public Builder nodeItem(ConfigNodeItem val) {
            nodeItem = val;
            return this;
        }

        /**
         * Returns a {@code ConfigProject} built from the parameters previously set.
         *
         * @return a {@code ConfigProject} built with parameters of this {@code ConfigProject.Builder}
         */
        public ConfigProject build() {
            return new ConfigProject(this);
        }
    }
}