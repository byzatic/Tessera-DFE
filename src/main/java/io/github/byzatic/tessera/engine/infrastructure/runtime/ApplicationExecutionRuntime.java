package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.commons.schedulers.cron.CronScheduler;
import io.github.byzatic.commons.schedulers.cron.CronSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.ImmediateScheduler;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.unified.ShutdownPolicy;
import io.github.byzatic.commons.schedulers.unified.UnifiedScheduler;
import io.github.byzatic.commons.schedulers.unified.UnifiedSchedulerInterface;

import java.time.Duration;
import java.time.ZoneId;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-scoped execution composition root.
 *
 * <p>Exactly one {@link ThreadPoolExecutor} executes application work. Scheduler facades and
 * serial lanes borrow this runtime and therefore cannot create or close additional worker pools.</p>
 */
public final class ApplicationExecutionRuntime implements AutoCloseable {

    private static final int DEFAULT_THREADS_PER_PROCESSOR = 8;
    private static final int DEFAULT_MINIMUM_MAXIMUM_THREADS = 32;
    private static final int DEFAULT_MAXIMUM_THREADS_LIMIT = 256;
    private static final Duration DEFAULT_KEEP_ALIVE = Duration.ofSeconds(60L);
    private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofMinutes(3L);

    private final UnifiedScheduler scheduler;

    private ApplicationExecutionRuntime(UnifiedScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public static ApplicationExecutionRuntime createDefault() {
        int processors = Runtime.getRuntime().availableProcessors();
        int coreThreads = readPositiveInt(
                "executionCoreThreads",
                Math.max(4, processors)
        );
        int threadsPerProcessor = readPositiveInt(
                "executionThreadsPerProcessor",
                DEFAULT_THREADS_PER_PROCESSOR
        );
        int maximumThreads = readPositiveInt(
                "executionMaximumThreads",
                calculateDefaultMaximumThreads(
                        processors,
                        coreThreads,
                        threadsPerProcessor
                )
        );
        if (maximumThreads < coreThreads) {
            throw new IllegalArgumentException(
                    "executionMaximumThreads must be greater than or equal to executionCoreThreads"
            );
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreThreads,
                maximumThreads,
                DEFAULT_KEEP_ALIVE.toMillis(),
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new ApplicationWorkerThreadFactory(),
                new WorkerWorkFirstPolicy()
        );
        executor.allowCoreThreadTimeOut(true);

        UnifiedScheduler scheduler = UnifiedScheduler.builder()
                .executor(executor)
                .timerThreadName("tessera-scheduler-timer")
                .shutdownPolicy(ShutdownPolicy.builder()
                        .gracefulTimeout(DEFAULT_SHUTDOWN_TIMEOUT)
                        .forcedTimeout(Duration.ofSeconds(10L))
                        .build())
                .build();
        return new ApplicationExecutionRuntime(scheduler);
    }

    static int calculateDefaultMaximumThreads(
            int processors,
            int coreThreads,
            int threadsPerProcessor
    ) {
        if (processors <= 0) {
            throw new IllegalArgumentException("processors must be greater than zero");
        }
        if (coreThreads <= 0) {
            throw new IllegalArgumentException("coreThreads must be greater than zero");
        }
        if (threadsPerProcessor <= 0) {
            throw new IllegalArgumentException(
                    "threadsPerProcessor must be greater than zero"
            );
        }

        long scaledMaximum = (long) processors * threadsPerProcessor;
        long boundedMaximum = Math.min(
                DEFAULT_MAXIMUM_THREADS_LIMIT,
                Math.max(DEFAULT_MINIMUM_MAXIMUM_THREADS, scaledMaximum)
        );
        return Math.max(coreThreads, (int) boundedMaximum);
    }

    public UnifiedSchedulerInterface scheduler() {
        return scheduler;
    }

    public ImmediateSchedulerInterface immediateScheduler() {
        return ImmediateScheduler.adapt(scheduler);
    }

    public CronSchedulerInterface cronScheduler(Duration cancellationGrace) {
        return CronScheduler.adapt(scheduler, ZoneId.systemDefault(), cancellationGrace);
    }

    @Override
    public void close() {
        scheduler.close();
    }

    private static int readPositiveInt(String property, int defaultValue) {
        String configured = System.getProperty(property);
        if (configured == null) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(configured);
            if (value <= 0) {
                throw new IllegalArgumentException(property + " must be greater than zero");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(property + " must be a whole number", exception);
        }
    }

    private static final class ApplicationWorkerThreadFactory implements ThreadFactory {

        private static final ThreadLocal<Boolean> APPLICATION_WORKER = new ThreadLocal<>();
        private final AtomicLong sequence = new AtomicLong();

        private static boolean isApplicationWorker() {
            return Boolean.TRUE.equals(APPLICATION_WORKER.get());
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                    () -> {
                        APPLICATION_WORKER.set(Boolean.TRUE);
                        try {
                            runnable.run();
                        } finally {
                            APPLICATION_WORKER.remove();
                        }
                    },
                    "tessera-worker-" + sequence.incrementAndGet()
            );
            thread.setDaemon(false);
            return thread;
        }
    }

    /**
     * Prevents thread-starvation when an application worker submits work that it must await.
     * External overload and submissions after shutdown retain the explicit rejection contract.
     */
    private static final class WorkerWorkFirstPolicy extends ThreadPoolExecutor.AbortPolicy {

        @Override
        public void rejectedExecution(Runnable command, ThreadPoolExecutor executor) {
            if (!executor.isShutdown() && ApplicationWorkerThreadFactory.isApplicationWorker()) {
                command.run();
                return;
            }
            throw new RejectedExecutionException(
                    "Tessera execution pool is saturated or shut down"
            );
        }
    }
}
