package io.github.byzatic.tessera.engine.infrastructure.service.service_manager.sheduller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import ru.byzatic.metrics_core.service_lib.service.health.HealthFlagState;
import ru.byzatic.metrics_core.service_lib.service.health.HealthFlagProxy;

import java.util.*;

public class Scheduler implements SchedulerInterface {
    private final static Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final Map<String, JobDetail> jobDetails = new HashMap<>();
    private final Map<String, Thread> threadMap = new HashMap<>();
    private final Map<String, HealthFlagProxy> healthFlagProxies = new HashMap<>();

    public Scheduler() {
    }

    @Override
    public synchronized void addJob(JobDetail jobDetail) throws OperationIncompleteException {
        ObjectsUtils.requireNonNull(jobDetail, new IllegalArgumentException(JobDetail.class.getSimpleName() + " should be not null"));

        boolean neeToAdd = Boolean.TRUE;
        if (jobDetails.containsKey(jobDetail.getUniqueId())) {
            if (jobDetails.get(jobDetail.getUniqueId()).equals(jobDetail)) {
                logger.debug("{} with id {} already exists in {} storage", JobDetail.class.getSimpleName(), jobDetail.getUniqueId(), Scheduler.class.getSimpleName());
                neeToAdd = Boolean.FALSE;
            } else {
                throw new OperationIncompleteException("Other " + JobDetail.class.getSimpleName() + " with id " + jobDetail.getUniqueId() + " already contains in " + Scheduler.class.getSimpleName() + " storage");
            }
        }

        if (neeToAdd) {
            jobDetails.put(jobDetail.getUniqueId(), jobDetail);
            logger.debug("{} added to {}", JobDetail.class.getSimpleName(), Scheduler.class.getSimpleName());
        }
    }

    @Override
    public synchronized void removeAllJobs(Long defaultForcedTerminationIntervalMinutes) throws OperationIncompleteException {
        for (Map.Entry<String, JobDetail> jobDetailEntry : jobDetails.entrySet()) {
            removeJob(jobDetailEntry.getValue(), defaultForcedTerminationIntervalMinutes);
        }
    }

    @Override
    public synchronized void removeJob(JobDetail jobDetail, Long defaultForcedTerminationIntervalMinutes) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(jobDetail, new IllegalArgumentException(JobDetail.class.getSimpleName() + " should be not null"));

            String jobId = jobDetail.getUniqueId();

            if (! jobDetails.containsKey(jobId)) {
                throw new OperationIncompleteException("No such registered Job with id " + jobId);
            }

            if (defaultForcedTerminationIntervalMinutes == null) {
                defaultForcedTerminationIntervalMinutes = 3L;
            }

            Boolean jobState = isJobActive(jobDetail);

            if (jobState) {
                Long forcedTerminationIntervalMinutes = jobDetail.getJob().getTerminationIntervalMinutes();
                if (forcedTerminationIntervalMinutes == null) {
                    forcedTerminationIntervalMinutes = defaultForcedTerminationIntervalMinutes;
                }

                jobDetail.getJob().terminate();
                long startTime = System.currentTimeMillis();

                do {
                    jobState = isJobActive(jobDetail);
                    if (! jobState) {
                        logger.debug("Job with id {} stopped", jobDetail);
                        break;
                    }
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime >= forcedTerminationIntervalMinutes * 60 * 1000) { // forcedTerminationIntervalMinutes минут в миллисекундах
                        logger.warn("More than {} minutes (forced termination interval) have passed", forcedTerminationIntervalMinutes);
                        threadMap.get(jobId).interrupt();
                        logger.warn("Job with id {} interrupted", jobId);
                    }
                    Thread.sleep(2);
                } while (jobState == Boolean.TRUE); // TODO: Value 'jobState' is always 'true' -> WTF?
            } else {
                logger.warn("Job with id {} not running", jobId);
            }

            threadMap.remove(jobId);
            logger.warn("Thread with id {} removed", jobId);

        } catch (Exception e ) {
            throw new OperationIncompleteException(e, e.getMessage());
        }
    }

    @Override
    public void runAllJobs(Boolean isJoinThreads) throws OperationIncompleteException {
        logger.debug("Requested run all jobs with isJoinThreads: {}", isJoinThreads);
        if (isJoinThreads == null) {
            isJoinThreads = Boolean.FALSE;
        }

        // Создаем поток для каждой задачи и запускаем их
        for (Map.Entry<String, JobDetail> jobDetailEntry : jobDetails.entrySet()) {
            JobDetail jobDetail = jobDetailEntry.getValue();
            String jobId = jobDetailEntry.getKey();

            if (threadMap.containsKey(jobId)) {
                logger.debug("Job with id {} is already running", jobId);
            } else {
                Thread thread = new Thread(jobDetail.getJob());
                thread.start();
                logger.debug("Job with id {} starts", jobId);
                threadMap.put(jobId, thread);
                logger.debug("Thread saved with id {}", jobId);
                healthFlagProxies.put(jobId, jobDetail.getHealthFlagProxy());
                logger.debug("HealthStateProxy saved with id {}", jobId);
            }
        }

        if (isJoinThreads) {
            // Ожидаем завершения всех потоков
            for (Map.Entry<String, Thread> threadSet : threadMap.entrySet()) {
                try {
                    threadSet.getValue().join();
                } catch (InterruptedException e) {
                    logger.debug("Thread was interrupted: " + e.getMessage());
                } catch (Exception e) {
                    throw new OperationIncompleteException(e, e.getMessage());
                }
            }
        }
    }

    @Override
    public synchronized Boolean isJobActive(JobDetail jobDetail) throws OperationIncompleteException {
        boolean result = Boolean.TRUE;

        ObjectsUtils.requireNonNull(jobDetail, new IllegalArgumentException(JobDetail.class.getSimpleName() + " should be not null"));

        if (! threadMap.containsKey(jobDetail.getUniqueId())) {
            throw new OperationIncompleteException("No such registered Job with id " + jobDetail.getUniqueId());
        }

        String jobUniqueId = jobDetail.getUniqueId();
        HealthFlagState healthFlag = healthFlagProxies.get(jobUniqueId).getHealthFlagState();
        Thread thread = threadMap.get(jobUniqueId);

        if (healthFlag == HealthFlagState.STOPPED) {
            result = Boolean.FALSE;
        }

        if (healthFlag == HealthFlagState.FATAL) {
            logger.warn("Job with id {} (module {}) has state FATAL", jobDetail.getJob(), jobDetail.getJob().getName());
            result = Boolean.FALSE;
        }

        if (! thread.isAlive()) {
            result = Boolean.FALSE;
        }

        return result;
    }

    @Override
    public List<JobDetail> getJobDetails() {
        return new ArrayList<>(jobDetails.values());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scheduler scheduler = (Scheduler) o;
        return Objects.equals(jobDetails, scheduler.jobDetails) && Objects.equals(threadMap, scheduler.threadMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobDetails, threadMap);
    }

    @Override
    public String toString() {
        return "Scheduler{" +
                "jobDetails=" + jobDetails +
                ", threadMap=" + threadMap +
                '}';
    }
}
