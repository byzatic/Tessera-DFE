package io.github.byzatic.tessera.engine.domain.business.sheduller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;

import java.util.*;

public class Scheduler implements SchedulerInterface {
    private final static Logger logger = LoggerFactory.getLogger(Scheduler.class);
    private final Map<Date, JobDetail> tasksMap = new TreeMap<>();

    @Override
    public synchronized void addTask(JobDetail jobDetail) {
        tasksMap.put(jobDetail.getNextExecutionDate(new Date()), jobDetail);
    }

    // Метод для получения всех задач до определенной даты
    private List<JobDetail> getTasksBefore(Date date) {
        Map<Date, JobDetail> tasksBefore = ((TreeMap<Date, JobDetail>) tasksMap).headMap(date);
        return new ArrayList<>(tasksBefore.values());
    }

    // Метод для изменения даты задачи по JobDetail
    private void updateTaskDate(JobDetail jobDetail, Date newDate) {
        // Находим текущую дату для указанного JobDetail
        Date currentDate = null;
        for (Map.Entry<Date, JobDetail> entry : tasksMap.entrySet()) {
            if (entry.getValue().equals(jobDetail)) {
                currentDate = entry.getKey();
                break;
            }
        }

        // Если задача с таким JobDetail найдена
        if (currentDate != null) {
            // Удаляем старую запись
            tasksMap.remove(currentDate);
            // Добавляем новую запись с обновленной датой
            tasksMap.put(newDate, jobDetail);
        }
    }

    @Override
    public void runAllTasks(Boolean isJoinThreads) {
        List<JobDetail> tasks = getTasksBefore(new Date());

        List<Thread> threads = new ArrayList<>();

        // Создаем поток для каждой задачи и запускаем их
        for (JobDetail task : tasks) {
            StatusResult.Status status = task.getStatusProxy().getStatusResult().getStatus();
            if (status == StatusResult.Status.NEVER_RUN || status == StatusResult.Status.COMPLETE) {
                Thread thread = new Thread(task.getJob());
                threads.add(thread);
                thread.start();
            }
            updateTaskDate(task, task.getNextExecutionDate(new Date()));
        }

        if (isJoinThreads) {
            // Ожидаем завершения всех потоков
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    logger.debug("Thread was interrupted: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void checkHealth() throws OperationIncompleteException {
        for (Map.Entry<Date, JobDetail> jobDetailEntry : tasksMap.entrySet()) {
            JobDetail jobDetail = jobDetailEntry.getValue();
            StatusResult statusResult = jobDetail.getStatusProxy().getStatusResult();
            if (statusResult.getStatus() == StatusResult.Status.FAULT) {
                Throwable cause = statusResult.getFaultCause();
                if (cause == null) {
                    logger.error("Job {} failed", jobDetail.getUniqueId());
                    throw new OperationIncompleteException("Job "+ jobDetail.getUniqueId() + " failed");
                } else {
                    logger.error("Job {} error trace: ", jobDetail.getUniqueId(), cause);
                    throw new OperationIncompleteException("Job "+ jobDetail.getUniqueId() + " failed because " + cause.getMessage());
                }
            }
        }
    }
}
