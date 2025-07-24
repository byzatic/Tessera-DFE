package io.github.byzatic.tessera.engine.domain.model.node_global;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class OptionsItem {

	@SerializedName("value")
	private String value;

	@SerializedName("key")
	private String key;

	public OptionsItem() {
	}

	private OptionsItem(Builder builder) {
		value = builder.value;
		key = builder.key;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(OptionsItem copy) {
		Builder builder = new Builder();
		builder.value = copy.getValue();
		builder.key = copy.getKey();
		return builder;
	}

	public String getValue(){
		return value;
	}

	public String getKey(){
		return key;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OptionsItem that = (OptionsItem) o;
		return Objects.equals(value, that.value) && Objects.equals(key, that.key);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, key);
	}

	@Override
	public String toString() {
		return "OptionsItem{" +
				"value='" + value + '\'' +
				", key='" + key + '\'' +
				'}';
	}

	/**
	 * {@code OptionsItem} builder static inner class.
	 */
	public static final class Builder {
		private String value;
		private String key;

		private Builder() {
		}

		/**
		 * Sets the {@code value} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code value} to set
		 * @return a reference to this Builder
		 */
		public Builder value(String val) {
			value = val;
			return this;
		}

		/**
		 * Sets the {@code key} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code key} to set
		 * @return a reference to this Builder
		 */
		public Builder key(String val) {
			key = val;
			return this;
		}

		/**
		 * Returns a {@code OptionsItem} built from the parameters previously set.
		 *
		 * @return a {@code OptionsItem} built with parameters of this {@code OptionsItem.Builder}
		 */
		public OptionsItem build() {
			return new OptionsItem(this);
		}
	}
}