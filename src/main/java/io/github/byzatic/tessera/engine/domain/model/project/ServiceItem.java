package io.github.byzatic.tessera.engine.domain.model.project;

import java.util.List;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class ServiceItem {

	@SerializedName("options")
	private List<ServicesOptionsItem> options;

	@SerializedName("description")
	private String description;

	@SerializedName("id_name")
	private String idName;

	public ServiceItem() {
	}

	private ServiceItem(Builder builder) {
		options = builder.options;
		description = builder.description;
		idName = builder.idName;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(ServiceItem copy) {
		Builder builder = new Builder();
		builder.options = copy.getOptions();
		builder.description = copy.getDescription();
		builder.idName = copy.getIdName();
		return builder;
	}

	public List<ServicesOptionsItem> getOptions(){
		return options;
	}

	public String getDescription(){
		return description;
	}

	public String getIdName(){
		return idName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ServiceItem that = (ServiceItem) o;
		return Objects.equals(options, that.options) && Objects.equals(description, that.description) && Objects.equals(idName, that.idName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(options, description, idName);
	}

	@Override
	public String toString() {
		return "ServiceItem{" +
				"options=" + options +
				", description='" + description + '\'' +
				", idName='" + idName + '\'' +
				'}';
	}

	/**
	 * {@code ServiceItem} builder static inner class.
	 */
	public static final class Builder {
		private List<ServicesOptionsItem> options;
		private String description;
		private String idName;

		private Builder() {
		}

		/**
		 * Sets the {@code options} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code options} to set
		 * @return a reference to this Builder
		 */
		public Builder options(List<ServicesOptionsItem> val) {
			options = val;
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
		 * Sets the {@code idName} and returns a reference to this Builder so that the methods can be chained together.
		 *
		 * @param val the {@code idName} to set
		 * @return a reference to this Builder
		 */
		public Builder idName(String val) {
			idName = val;
			return this;
		}

		/**
		 * Returns a {@code ServiceItem} built from the parameters previously set.
		 *
		 * @return a {@code ServiceItem} built with parameters of this {@code ServiceItem.Builder}
		 */
		public ServiceItem build() {
			return new ServiceItem(this);
		}
	}
}