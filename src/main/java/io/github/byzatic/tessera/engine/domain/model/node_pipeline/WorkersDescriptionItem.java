package io.github.byzatic.tessera.engine.domain.model.node_pipeline;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class WorkersDescriptionItem{

	@SerializedName("name")
	private String name;

	@SerializedName("description")
	private String description;

	@SerializedName("configuration_files")
	private List<ConfigurationFilesItem> configurationFiles;

	public WorkersDescriptionItem() {
	}

	private WorkersDescriptionItem(Builder builder) {
		name = builder.name;
		description = builder.description;
		configurationFiles = builder.configurationFiles;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(WorkersDescriptionItem copy) {
		Builder builder = new Builder();
		builder.name = copy.getName();
		builder.description = copy.getDescription();
		builder.configurationFiles = copy.getConfigurationFiles();
		return builder;
	}

	public String getName(){
		return name;
	}

	public String getDescription(){
		return description;
	}

	public List<ConfigurationFilesItem> getConfigurationFiles(){
		return configurationFiles;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WorkersDescriptionItem that = (WorkersDescriptionItem) o;
		return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(configurationFiles, that.configurationFiles);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, description, configurationFiles);
	}

	@Override
	public String toString() {
		return "WorkersDescriptionItem{" +
				"name='" + name + '\'' +
				", description='" + description + '\'' +
				", configurationFiles=" + configurationFiles +
				'}';
	}

	/**
	 * {@code WorkersDescriptionItem} builder static inner class.
	 */
	public static final class Builder {
		private String name;
		private String description;
		private List<ConfigurationFilesItem> configurationFiles;

		private Builder() {
		}

		/**
		 * Sets the {@code name} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code name} to set
		 * @return a reference to this Builder
		 */
		public Builder name(String val) {
			name = val;
			return this;
		}

		/**
		 * Sets the {@code description} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code description} to set
		 * @return a reference to this Builder
		 */
		public Builder description(String val) {
			description = val;
			return this;
		}

		/**
		 * Sets the {@code configurationFiles} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code configurationFiles} to set
		 * @return a reference to this Builder
		 */
		public Builder configurationFiles(List<ConfigurationFilesItem> val) {
			configurationFiles = val;
			return this;
		}

		/**
		 * Returns a {@code WorkersDescriptionItem} built from the parameters previously set.
		 *
		 * @return a {@code WorkersDescriptionItem} built with parameters of this {@code WorkersDescriptionItem.Builder}
		 */
		public WorkersDescriptionItem build() {
			return new WorkersDescriptionItem(this);
		}
	}
}