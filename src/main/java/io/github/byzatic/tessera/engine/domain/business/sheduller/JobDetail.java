package io.github.byzatic.tessera.engine.domain.business.sheduller;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class JobDetail {
    private String uniqueId;
    private Runnable job;
    private  CronDateCalculator calculator;
    private StatusProxy statusProxy;

    public JobDetail() {
    }

    private JobDetail(Builder builder) {
        uniqueId = builder.uniqueId;
        job = builder.job;
        calculator = builder.calculator;
        statusProxy = builder.statusProxy;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(JobDetail copy) {
        Builder builder = new Builder();
        builder.uniqueId = copy.uniqueId;
        builder.job = copy.job;
        builder.calculator = copy.calculator;
        builder.statusProxy = copy.statusProxy;
        return builder;
    }


    public String getUniqueId() {
        return uniqueId;
    }

    public Runnable getJob() {
        return job;
    }

    public Date getNextExecutionDate(Date now) {
        return calculator.getNextExecutionDate(now);
    }

    public StatusProxy getStatusProxy() {
        return statusProxy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobDetail jobDetail = (JobDetail) o;
        return Objects.equals(uniqueId, jobDetail.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public String toString() {
        return "JobDetail{" +
                "uniqueId='" + uniqueId + '\'' +
                ", job=" + job +
                ", calculator=" + calculator +
                ", statusProxy=" + statusProxy +
                '}';
    }

    /**
     * {@code JobDetail} builder static inner class.
     */
    public static final class Builder {
        private String uniqueId = UUID.randomUUID().toString();
        private Runnable job;
        private CronDateCalculator calculator;
        private StatusProxy statusProxy = new StatusProxy(uniqueId);

        private Builder() {
        }

        /**
         * Sets the {@code uniqueId} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param uniqueId the {@code uniqueId} to set
         * @return a reference to this Builder
         */
        public Builder setUniqueId(String uniqueId) {
            this.uniqueId = uniqueId;
            return this;
        }

        /**
         * Sets the {@code job} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param job the {@code job} to set
         * @return a reference to this Builder
         */
        public Builder setJob(Runnable job) {
            this.job = new JobWrapper(this.statusProxy, job, this.uniqueId, null);
            return this;
        }

        public Builder setJob(JobWrapper jobWrapper) {
            this.job = jobWrapper;
            return this;
        }

        /**
         * Sets the {@code calculator} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param cronExpressionString the {@code cronExpressionString} to set {@code calculator}
         * @return a reference to this Builder
         */
        public Builder setCronExpressionString(String cronExpressionString) throws ParseException {
            this.calculator = new CronDateCalculator(cronExpressionString);
            return this;
        }

        /**
         * Sets the {@code statusProxy} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param statusProxy the {@code statusProxy} to set
         * @return a reference to this Builder
         */
        public Builder setStatusProxy(StatusProxy statusProxy) {
            this.statusProxy = statusProxy;
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
