package io.github.byzatic.tessera.engine.domain.model.node_pipeline;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public class StagesDescriptionItem {

    @SerializedName("workers_description")
    private List<WorkersDescriptionItem> workersDescription;

    @SerializedName("stage_id")
    private String stageId;

    public StagesDescriptionItem() {
    }

    private StagesDescriptionItem(Builder builder) {
        workersDescription = builder.workersDescription;
        stageId = builder.stageId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(StagesDescriptionItem copy) {
        Builder builder = new Builder();
        builder.workersDescription = copy.getWorkersDescription();
        builder.stageId = copy.getStageId();
        return builder;
    }


    public List<WorkersDescriptionItem> getWorkersDescription() {
        return workersDescription;
    }

    public String getStageId() {
        return stageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StagesDescriptionItem that = (StagesDescriptionItem) o;
        return Objects.equals(workersDescription, that.workersDescription) && Objects.equals(stageId, that.stageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workersDescription, stageId);
    }

    @Override
    public String toString() {
        return "StagesDescriptionItem{" +
                "workersDescription=" + workersDescription +
                ", stageId='" + stageId + '\'' +
                '}';
    }

    /**
     * {@code StagesDescriptionItem} builder static inner class.
     */
    public static final class Builder {
        private List<WorkersDescriptionItem> workersDescription;
        private String stageId;

        private Builder() {
        }

        /**
         * Sets the {@code workersDescription} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code workersDescription} to set
         * @return a reference to this Builder
         */
        public Builder workersDescription(List<WorkersDescriptionItem> val) {
            workersDescription = val;
            return this;
        }

        /**
         * Sets the {@code stageId} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code stageId} to set
         * @return a reference to this Builder
         */
        public Builder stageId(String val) {
            stageId = val;
            return this;
        }

        /**
         * Returns a {@code StagesDescriptionItem} built from the parameters previously set.
         *
         * @return a {@code StagesDescriptionItem} built with parameters of this {@code StagesDescriptionItem.Builder}
         */
        public StagesDescriptionItem build() {
            return new StagesDescriptionItem(this);
        }
    }
}