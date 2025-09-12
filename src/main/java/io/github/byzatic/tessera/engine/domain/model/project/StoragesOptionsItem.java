package io.github.byzatic.tessera.engine.domain.model.project;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class StoragesOptionsItem {

    @SerializedName("key")
    private String key;

    @SerializedName("value")
    private String value;

    public StoragesOptionsItem() {
    }

    private StoragesOptionsItem(Builder builder) {
        key = builder.data;
        value = builder.name;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(StoragesOptionsItem copy) {
        Builder builder = new Builder();
        builder.data = copy.getKey();
        builder.name = copy.getValue();
        return builder;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoragesOptionsItem that = (StoragesOptionsItem) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "OptionsItem{" +
                "data='" + key + '\'' +
                ", name='" + value + '\'' +
                '}';
    }

    /**
     * {@code OptionsItem} builder static inner class.
     */
    public static final class Builder {
        private String data;
        private String name;

        private Builder() {
        }

        /**
         * Sets the {@code data} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code data} to set
         * @return a reference to this Builder
         */
        public Builder data(String val) {
            data = val;
            return this;
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
         * Returns a {@code OptionsItem} built from the parameters previously set.
         *
         * @return a {@code OptionsItem} built with parameters of this {@code OptionsItem.Builder}
         */
        public StoragesOptionsItem build() {
            return new StoragesOptionsItem(this);
        }
    }
}