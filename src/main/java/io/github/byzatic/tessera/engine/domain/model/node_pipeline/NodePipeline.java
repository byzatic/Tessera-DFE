package io.github.byzatic.tessera.engine.domain.model.node_pipeline;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class NodePipeline {

	@SerializedName("stages_consistency")
	private List<StagesConsistencyItem> stagesConsistency;

	@SerializedName("stages_description")
	private List<StagesDescriptionItem> stagesDescription;

	public NodePipeline() {
	}

	private NodePipeline(Builder builder) {
		stagesConsistency = builder.stagesConsistency;
		stagesDescription = builder.stagesDescription;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(NodePipeline copy) {
		Builder builder = new Builder();
		builder.stagesConsistency = copy.getStagesConsistency();
		builder.stagesDescription = copy.getStagesDescription();
		return builder;
	}

	public List<StagesConsistencyItem> getStagesConsistency(){
		return stagesConsistency;
	}

	public List<StagesDescriptionItem> getStagesDescription(){
		return stagesDescription;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		NodePipeline that = (NodePipeline) o;
		return Objects.equals(stagesConsistency, that.stagesConsistency) && Objects.equals(stagesDescription, that.stagesDescription);
	}

	@Override
	public int hashCode() {
		return Objects.hash(stagesConsistency, stagesDescription);
	}

	@Override
	public String toString() {
		return "NodePipeline{" +
				"stagesConsistency=" + stagesConsistency +
				", stagesDescription=" + stagesDescription +
				'}';
	}

	/**
	 * {@code NodePipeline} builder static inner class.
	 */
	public static final class Builder {
		private List<StagesConsistencyItem> stagesConsistency;
		private List<StagesDescriptionItem> stagesDescription;

		private Builder() {
		}

		/**
		 * Sets the {@code stagesConsistency} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code stagesConsistency} to set
		 * @return a reference to this Builder
		 */
		public Builder stagesConsistency(List<StagesConsistencyItem> val) {
			stagesConsistency = val;
			return this;
		}

		/**
		 * Sets the {@code stagesDescription} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code stagesDescription} to set
		 * @return a reference to this Builder
		 */
		public Builder stagesDescription(List<StagesDescriptionItem> val) {
			stagesDescription = val;
			return this;
		}

		/**
		 * Returns a {@code NodePipeline} built from the parameters previously set.
		 *
		 * @return a {@code NodePipeline} built with parameters of this {@code NodePipeline.Builder}
		 */
		public NodePipeline build() {
			return new NodePipeline(this);
		}
	}
}