package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller;

import ru.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.health.HealthStateProxy;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.job.JobInterface;

import java.util.Objects;
import java.util.UUID;

public class JobDetail {
    private String uniqueId = null;
    private JobInterface job = null;
    private HealthStateProxy healthStateProxy = null;

    private JobDetail() {
    }

    private JobDetail(Builder builder) {
        uniqueId = Objects.requireNonNullElseGet(builder.uniqueId, () -> UUID.randomUUID().toString().replace("-", ""));

        ObjectsUtils.requireNonNull(builder.job, new IllegalArgumentException("job should be NotNull"));
        job = builder.job;

        ObjectsUtils.requireNonNull(builder.job, new IllegalArgumentException("HealthStateProxy should be NotNull"));
        healthStateProxy = builder.HealthStateProxy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(JobDetail copy) {
        Builder builder = new Builder();
        builder.uniqueId = copy.getUniqueId();
        builder.job = copy.getJob();
        builder.HealthStateProxy = copy.getHealthStateProxy();
        return builder;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public JobInterface getJob() {
        return job;
    }

    public HealthStateProxy getHealthStateProxy() {
        return healthStateProxy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDetail jobDetail = (JobDetail) o;
        return Objects.equals(uniqueId, jobDetail.uniqueId) && Objects.equals(job, jobDetail.job) && Objects.equals(healthStateProxy, jobDetail.healthStateProxy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, job, healthStateProxy);
    }

    @Override
    public String toString() {
        return "JobDetail{" +
                "uniqueId='" + uniqueId + '\'' +
                ", job=" + job +
                ", healthStateProxy=" + healthStateProxy +
                '}';
    }

    /**
     * {@code JobDetail} builder static inner class.
     */
    public static final class Builder {
        private String uniqueId;
        private JobInterface job;
        private HealthStateProxy HealthStateProxy;

        private Builder() {
        }

        /**
         * Sets the {@code uniqueId} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code uniqueId} to set
         * @return a reference to this Builder
         */
        public Builder uniqueId(String val) {
            uniqueId = val;
            return this;
        }

        /**
         * Sets the {@code job} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code job} to set
         * @return a reference to this Builder
         */
        public Builder job(JobInterface val) {
            job = val;
            return this;
        }

        /**
         * Sets the {@code HealthStateProxy} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code HealthStateProxy} to set
         * @return a reference to this Builder
         */
        public Builder HealthStateProxy(HealthStateProxy val) {
            HealthStateProxy = val;
            return this;
        }

        /**
         * Returns a {@code JobDetail} built from the parameters previously set.
         *
         * @return a {@code JobDetail} built with parameters of this {@code JobDetail.Builder}
         */
        public JobDetail build() {
            return new JobDetail(this);
        }
    }
}
