package io.github.byzatic.tessera.engine.domain.model.node_pipeline;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class StagesConsistencyItem {

    @SerializedName("stage_id")
    private String stageId;

    @SerializedName("position")
    private int position;

    public StagesConsistencyItem() {
    }

    private StagesConsistencyItem(Builder builder) {
        stageId = builder.stageId;
        position = builder.position;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(StagesConsistencyItem copy) {
        Builder builder = new Builder();
        builder.stageId = copy.getStageId();
        builder.position = copy.getPosition();
        return builder;
    }

    public String getStageId() {
        return stageId;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StagesConsistencyItem that = (StagesConsistencyItem) o;
        return position == that.position && Objects.equals(stageId, that.stageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageId, position);
    }

    @Override
    public String toString() {
        return "StagesConsistencyItem{" +
                "stageId='" + stageId + '\'' +
                ", position=" + position +
                '}';
    }

    /**
     * {@code StagesConsistencyItem} builder static inner class.
     */
    public static final class Builder {
        private String stageId;
        private int position;

        private Builder() {
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
         * Sets the {@code position} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code position} to set
         * @return a reference to this Builder
         */
        public Builder position(int val) {
            position = val;
            return this;
        }

        /**
         * Returns a {@code StagesConsistencyItem} built from the parameters previously set.
         *
         * @return a {@code StagesConsistencyItem} built with parameters of this {@code StagesConsistencyItem.Builder}
         */
        public StagesConsistencyItem build() {
            return new StagesConsistencyItem(this);
        }
    }
}