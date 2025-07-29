package io.github.byzatic.tessera.engine.domain.model;

import io.github.byzatic.commons.ObjectsUtils;

import java.util.Objects;

public class DataLookupIdentifierImpl {
    private String dataLookupIdentifier = null;

    private DataLookupIdentifierImpl() {
    }

    private DataLookupIdentifierImpl(Builder builder) {
        ObjectsUtils.requireNonNull(builder.dataId, new IllegalArgumentException("dataId should be not null"));
        dataLookupIdentifier = builder.dataId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(DataLookupIdentifierImpl copy) {
        Builder builder = new Builder();
        builder.dataId = copy.getDataLookupIdentifier();
        return builder;
    }

    public String getDataLookupIdentifier() {
        return dataLookupIdentifier;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataLookupIdentifierImpl that = (DataLookupIdentifierImpl) o;
        return Objects.equals(dataLookupIdentifier, that.dataLookupIdentifier);
    }

    public int hashCode() {
        return Objects.hash(dataLookupIdentifier);
    }

    public String toString() {
        return "StorageItemIdIdImpl{" +
                "dataId='" + dataLookupIdentifier + '\'' +
                '}';
    }

    /**
     * {@code StorageItemIdIdImpl} builder static inner class.
     */
    public static final class Builder {
        private String dataId;

        private Builder() {
        }

        /**
         * Sets the {@code dataId} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code dataId} to set
         * @return a reference to this Builder
         */
        public Builder dataId(String val) {
            dataId = val;
            return this;
        }

        /**
         * Returns a {@code StorageItemIdIdImpl} built from the parameters previously set.
         *
         * @return a {@code StorageItemIdIdImpl} built with parameters of this {@code StorageItemIdIdImpl.Builder}
         */
        public DataLookupIdentifierImpl build() {
            return new DataLookupIdentifierImpl(this);
        }
    }
}
