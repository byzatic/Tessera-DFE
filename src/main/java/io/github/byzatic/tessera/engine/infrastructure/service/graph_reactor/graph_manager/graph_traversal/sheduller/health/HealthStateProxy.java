package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.health;

import java.util.Objects;

public class HealthStateProxy {
    private HealthFlag healthFlag = null;
    private Throwable throwable = null;

    private HealthStateProxy() {
    }

    private HealthStateProxy(Builder builder) {
        setHealthFlag(builder.healthFlag);
        setThrowable(builder.throwable);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(HealthStateProxy copy) {
        Builder builder = new Builder();
        builder.healthFlag = copy.getHealthFlag();
        builder.throwable = copy.getThrowable();
        return builder;
    }

    public synchronized HealthFlag getHealthFlag() {
        return this.healthFlag;
    }

    public synchronized void setHealthFlag(HealthFlag healthFlag) {
        this.healthFlag = healthFlag;
    }

    public synchronized Throwable getThrowable() {
        return this.throwable;
    }

    public synchronized void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthStateProxy that = (HealthStateProxy) o;
        return healthFlag == that.healthFlag && Objects.equals(throwable, that.throwable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(healthFlag, throwable);
    }

    @Override
    public String toString() {
        return "HealthStateProxy{" +
                "healthFlag=" + healthFlag +
                ", throwable=" + throwable +
                '}';
    }

    /**
     * {@code HealthStateProxy} builder static inner class.
     */
    public static final class Builder {
        private HealthFlag healthFlag;
        private Throwable throwable;

        private Builder() {
        }

        /**
         * Sets the {@code healthFlag} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param healthFlag the {@code healthFlag} to set
         * @return a reference to this Builder
         */
        public Builder setHealthFlag(HealthFlag healthFlag) {
            this.healthFlag = healthFlag;
            return this;
        }

        /**
         * Sets the {@code throwable} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param throwable the {@code throwable} to set
         * @return a reference to this Builder
         */
        public Builder setThrowable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        /**
         * Returns a {@code HealthStateProxy} built from the parameters previously set.
         *
         * @return a {@code HealthStateProxy} built with parameters of this {@code HealthStateProxy.Builder}
         */
        public HealthStateProxy build() {
            return new HealthStateProxy(this);
        }
    }
}
