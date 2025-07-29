package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.pipeline_manager.sheduller;

import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.health.HealthFlagProxy;
import io.github.byzatic.tessera.workflowroutine.workflowroutines.WorkflowRoutineInterface;

import java.util.Objects;
import java.util.UUID;

public class JobDetail {
    private String uniqueId = null;
    private WorkflowRoutineInterface job = null;
    private HealthFlagProxy healthFlagProxy = null;

    private JobDetail() {
    }

    private JobDetail(Builder builder) {
        uniqueId = Objects.requireNonNullElseGet(builder.uniqueId, () -> UUID.randomUUID().toString().replace("-", ""));
        ObjectsUtils.requireNonNull(builder.job, new IllegalArgumentException("job should be NotNull"));
        job = builder.job;
        ObjectsUtils.requireNonNull(builder.job, new IllegalArgumentException("healthFlagProxy should be NotNull"));
        healthFlagProxy = builder.healthFlagProxy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(JobDetail copy) {
        Builder builder = new Builder();
        builder.uniqueId = copy.getUniqueId();
        builder.job = copy.getJob();
        builder.healthFlagProxy = copy.getHealthFlagProxy();
        return builder;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public WorkflowRoutineInterface getJob() {
        return job;
    }

    public HealthFlagProxy getHealthFlagProxy() {
        return healthFlagProxy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDetail jobDetail = (JobDetail) o;
        return Objects.equals(uniqueId, jobDetail.uniqueId) && Objects.equals(job, jobDetail.job) && Objects.equals(healthFlagProxy, jobDetail.healthFlagProxy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId, job, healthFlagProxy);
    }

    @Override
    public String toString() {
        return "JobDetail{" +
                "uniqueId='" + uniqueId + '\'' +
                ", job=" + job +
                ", healthFlagProxy=" + healthFlagProxy +
                '}';
    }

    /**
     * {@code JobDetail} builder static inner class.
     */
    public static final class Builder {
        private String uniqueId;
        private WorkflowRoutineInterface job;
        private HealthFlagProxy healthFlagProxy;

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
        public Builder job(WorkflowRoutineInterface val) {
            job = val;
            return this;
        }

        /**
         * Sets the {@code healthFlagProxy} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param val the {@code healthFlagProxy} to set
         * @return a reference to this Builder
         */
        public Builder healthFlagProxy(HealthFlagProxy val) {
            healthFlagProxy = val;
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
