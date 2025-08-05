package io.github.byzatic.tessera.engine.domain.model.project;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class ServicesOptionsItem {

	@SerializedName("data")
	private String data;

	@SerializedName("name")
	private String name;

	public ServicesOptionsItem() {
	}

	private ServicesOptionsItem(Builder builder) {
		data = builder.data;
		name = builder.name;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(ServicesOptionsItem copy) {
		Builder builder = new Builder();
		builder.data = copy.getData();
		builder.name = copy.getName();
		return builder;
	}

	public String getData(){
		return data;
	}

	public String getName(){
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServicesOptionsItem that = (ServicesOptionsItem) o;
		return Objects.equals(data, that.data) && Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(data, name);
	}

	@Override
	public String toString() {
		return "OptionsItem{" +
				"data='" + data + '\'' +
				", name='" + name + '\'' +
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
		public ServicesOptionsItem build() {
			return new ServicesOptionsItem(this);
		}
	}
}