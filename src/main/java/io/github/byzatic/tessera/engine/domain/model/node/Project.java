package io.github.byzatic.tessera.engine.domain.model.node;

import com.google.gson.annotations.SerializedName;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;

import java.util.Map;
import java.util.Objects;

public class Project {

	@SerializedName("project_config_version")
	private String projectConfigVersion;

	@SerializedName("project_name")
	private String projectName;

	@SerializedName("structure")
	private Map<GraphNodeRef, NodeItem> nodeMap;

	public Project() {
	}

	private Project(Builder builder) {
		projectConfigVersion = builder.projectConfigVersion;
		projectName = builder.projectName;
		nodeMap = builder.nodeMap;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(Project copy) {
		Builder builder = new Builder();
		builder.projectConfigVersion = copy.getProjectConfigVersion();
		builder.projectName = copy.getProjectName();
		builder.nodeMap = copy.getNodeMap();
		return builder;
	}

	public String getProjectConfigVersion(){
		return projectConfigVersion;
	}

	public String getProjectName(){
		return projectName;
	}

	public Map<GraphNodeRef, NodeItem> getNodeMap() {
		return nodeMap;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Project project = (Project) o;
		return Objects.equals(projectConfigVersion, project.projectConfigVersion) && Objects.equals(projectName, project.projectName) && Objects.equals(nodeMap, project.nodeMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(projectConfigVersion, projectName, nodeMap);
	}

	@Override
	public String toString() {
		return "ConfigProject{" +
				"projectConfigVersion='" + projectConfigVersion + '\'' +
				", projectName='" + projectName + '\'' +
				", nodeMap=" + nodeMap +
				'}';
	}

	/**
	 * {@code ConfigProject} builder static inner class.
	 */
	public static final class Builder {
		private String projectConfigVersion;
		private String projectName;
		private Map<GraphNodeRef, NodeItem> nodeMap;

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
		 * Sets the {@code nodeMap} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code nodeMap} to set
		 * @return a reference to this Builder
		 */
		public Builder nodeMap(Map<GraphNodeRef, NodeItem> val) {
			nodeMap = val;
			return this;
		}

		/**
		 * Returns a {@code ConfigProject} built from the parameters previously set.
		 *
		 * @return a {@code ConfigProject} built with parameters of this {@code ConfigProject.Builder}
		 */
		public Project build() {
			return new Project(this);
		}
	}
}