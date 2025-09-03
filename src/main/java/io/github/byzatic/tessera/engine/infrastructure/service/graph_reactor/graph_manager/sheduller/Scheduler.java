package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller;

import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.health.HealthFlag;
import io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.sheduller.health.HealthStateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

import java.util.*;

public class Scheduler implements SchedulerInterface {
    private final static Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final Map<String, JobDetail> jobDetails = new HashMap<>();
    private final Map<String, Thread> threadMap = new HashMap<>();
    private final Map<String, HealthStateProxy> healthStateProxies = new HashMap<>();

    public Scheduler() {
    }

    @Override
    public void addJob(JobDetail jobDetail) throws OperationIncompleteException {
        ObjectsUtils.requireNonNull(jobDetail, new IllegalArgumentException(JobDetail.class.getSimpleName() + " should be not null"));
        String jobId = jobDetail.getUniqueId();
        if (jobDetails.containsKey(jobId)) throw new OperationIncompleteException("Can't add job; job with id " + jobId + " already added");
        jobDetails.put(jobDetail.getUniqueId(), jobDetail);
        logger.debug("{} added to {}", JobDetail.class.getSimpleName(), Scheduler.class.getSimpleName());
    }

    @Override
    public void runAllJobs() throws OperationIncompleteException {
        logger.debug("Requested run all jobs");

        // Создаем и запускаем поток для каждой задачи
        for (Map.Entry<String, JobDetail> jobDetailEntry : jobDetails.entrySet()) {
            JobDetail jobDetail = jobDetailEntry.getValue();
            String jobId = jobDetailEntry.getKey();

            Thread thread = new Thread(jobDetail.getJob());
            thread.start();
            logger.debug("Job with id {} starts", jobId);

            threadMap.put(jobId, thread);
            logger.debug("Thread saved with id {}", jobId);

            healthStateProxies.put(jobId, jobDetail.getHealthStateProxy());
            logger.debug("HealthStateProxy saved with id {}", jobId);
        }

        // Ожидаем завершения всех потоков
        for (Map.Entry<String, Thread> threadSet : threadMap.entrySet()) {
            try {
                threadSet.getValue().join();
            } catch (Exception e) {
                throw new OperationIncompleteException(e, e.getMessage());
            }
        }

        // Проверяем состояние всех потоков
        for (Map.Entry<String, HealthStateProxy> healthStateProxyEntry : healthStateProxies.entrySet()) {
            String processId = healthStateProxyEntry.getKey();
            HealthStateProxy processHealthStateProxy = healthStateProxyEntry.getValue();

            switch (processHealthStateProxy.getHealthFlag()) {
                case ERROR -> {
                    String errMessage = "Process with id" + processId + " finished with error state";
                    logger.error(errMessage);
                    logger.error("Error process JobDetail: {}", jobDetails.get(processId));
                    throw new OperationIncompleteException(errMessage);
                }
                case RUNNING -> {
                    String errMessage = "Internal Logic error: Process with id" + processId + " have RUNNING state before ending thread.";
                    logger.error(errMessage);
                    logger.error("Error process JobDetail: {}", jobDetails.get(processId));
                    throw new OperationIncompleteException(errMessage);
                }
                case FINISHED -> logger.debug("Process with id {} complete", processId);
            }
        }

        // Очищаем данные потоков
        cleanupProcesses();
    }


    private void cleanupProcesses() throws OperationIncompleteException {
        for (Map.Entry<String, JobDetail> jobDetailEntry : jobDetails.entrySet()) {
            String processId = jobDetailEntry.getKey();
            JobDetail processJobDetail = jobDetailEntry.getValue();
            cleanupProcess(processId, processJobDetail);
        }
    }

    private void cleanupProcess(String processId, JobDetail processJobDetail) throws OperationIncompleteException {
        String jobStillActiveErrorMessage = "Can't remove Job with id "+processId+" cause process is still active; Job: "+processJobDetail;
        if (isJobActive(processJobDetail)) throw new OperationIncompleteException(jobStillActiveErrorMessage);
        threadMap.remove(processId);
        healthStateProxies.remove(processId);
    }

    @Override
    public void cleanup() throws OperationIncompleteException {
        for (Map.Entry<String, JobDetail> jobDetailEntry : jobDetails.entrySet()) {
            String processId = jobDetailEntry.getKey();
            JobDetail processJobDetail = jobDetailEntry.getValue();
            String jobStillActiveErrorMessage = "Can't remove Job with id "+processId+" cause process is still active; Job: "+processJobDetail;
            if (isJobActive(processJobDetail)) throw new OperationIncompleteException(jobStillActiveErrorMessage);
            cleanupProcess(processId, processJobDetail);
            jobDetails.remove(processId);
        }
    }

    @Override
    public synchronized Boolean isJobActive(JobDetail jobDetail) throws OperationIncompleteException {
        ObjectsUtils.requireNonNull(jobDetail, new IllegalArgumentException(JobDetail.class.getSimpleName() + " should be not null"));

        boolean result = Boolean.TRUE;

        String jobUniqueId = jobDetail.getUniqueId();

        if (threadMap.containsKey(jobDetail.getUniqueId())) {
            HealthFlag healthFlag = healthStateProxies.get(jobUniqueId).getHealthFlag();
            Thread thread = threadMap.get(jobUniqueId);

            if (healthFlag == HealthFlag.FINISHED) {
                result = Boolean.FALSE;
            }

            if (healthFlag == HealthFlag.ERROR) {
                result = Boolean.FALSE;
            }

            if (! thread.isAlive()) {
                result = Boolean.FALSE;
            }
        } else {
            result = Boolean.FALSE;
        }

        return result;
    }
}
