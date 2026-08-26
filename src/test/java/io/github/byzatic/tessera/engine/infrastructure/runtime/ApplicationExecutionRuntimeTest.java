package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.commons.schedulers.cron.CronSchedulerInterface;
import io.github.byzatic.commons.schedulers.immediate.ImmediateSchedulerInterface;
import io.github.byzatic.commons.schedulers.unified.RunState;
import org.junit.Test;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationExecutionRuntimeTest {

    @Test
    public void defaultMaximumThreadsScalesWithProcessorsWithinSafeBounds() {
        assertEquals(32, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(2, 4, 8));
        assertEquals(32, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(4, 4, 8));
        assertEquals(64, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(8, 8, 8));
        assertEquals(128, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(16, 16, 8));
        assertEquals(256, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(32, 32, 8));
        assertEquals(256, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(64, 64, 8));
    }

    @Test
    public void configuredCoreThreadsRemainTheLowerBound() {
        assertEquals(300, ApplicationExecutionRuntime.calculateDefaultMaximumThreads(8, 300, 8));
    }

    @Test
    public void legacyFacadesBorrowTheProcessWorkerPool() throws Exception {
        ApplicationExecutionRuntime runtime = ApplicationExecutionRuntime.createDefault();
        Set<String> workerNames = ConcurrentHashMap.newKeySet();
        try {
            ImmediateSchedulerInterface immediate = runtime.immediateScheduler();
            CountDownLatch immediateRun = new CountDownLatch(1);
            immediate.addTask(token -> {
                workerNames.add(Thread.currentThread().getName());
                immediateRun.countDown();
            });
            assertTrue(immediateRun.await(2L, TimeUnit.SECONDS));
            immediate.close();

            CronSchedulerInterface cron = runtime.cronScheduler(Duration.ofSeconds(1L));
            CountDownLatch cronRun = new CountDownLatch(1);
            cron.addJob("0 0 0 1 1 *", token -> {
                workerNames.add(Thread.currentThread().getName());
                cronRun.countDown();
            }, true, true);
            assertTrue(cronRun.await(2L, TimeUnit.SECONDS));
            cron.close();

            assertEquals(
                    RunState.COMPLETED,
                    runtime.scheduler().submit(() ->
                            workerNames.add(Thread.currentThread().getName())
                    ).await(Duration.ofSeconds(2L)).state()
            );
            assertTrue(workerNames.stream().allMatch(name -> name.startsWith("tessera-worker-")));
        } finally {
            runtime.close();
        }
    }

    @Test
    public void saturatedWorkerExecutesAwaitedChildWorkFirst() throws Exception {
        String previousCore = System.getProperty("executionCoreThreads");
        String previousMaximum = System.getProperty("executionMaximumThreads");
        System.setProperty("executionCoreThreads", "1");
        System.setProperty("executionMaximumThreads", "1");
        ApplicationExecutionRuntime runtime = null;
        try {
            runtime = ApplicationExecutionRuntime.createDefault();
            ApplicationExecutionRuntime currentRuntime = runtime;
            assertEquals(
                    RunState.COMPLETED,
                    runtime.scheduler().submit(cancellation ->
                            currentRuntime.scheduler().submit(() -> { })
                                    .await(Duration.ofSeconds(1L))
                    ).await(Duration.ofSeconds(2L)).state()
            );
        } finally {
            if (runtime != null) runtime.close();
            restoreProperty("executionCoreThreads", previousCore);
            restoreProperty("executionMaximumThreads", previousMaximum);
        }
    }

    @Test
    public void saturatedPoolStillRejectsExternalSubmissions() throws Exception {
        String previousCore = System.getProperty("executionCoreThreads");
        String previousMaximum = System.getProperty("executionMaximumThreads");
        System.setProperty("executionCoreThreads", "1");
        System.setProperty("executionMaximumThreads", "1");
        ApplicationExecutionRuntime runtime = null;
        CountDownLatch release = new CountDownLatch(1);
        try {
            runtime = ApplicationExecutionRuntime.createDefault();
            CountDownLatch started = new CountDownLatch(1);
            runtime.scheduler().submit(cancellation -> {
                started.countDown();
                release.await();
            });
            assertTrue(started.await(1L, TimeUnit.SECONDS));
            try {
                runtime.scheduler().submit(() -> { });
                org.junit.Assert.fail("External overload must remain explicit");
            } catch (RejectedExecutionException expected) {
                // expected
            }
        } finally {
            release.countDown();
            if (runtime != null) runtime.close();
            restoreProperty("executionCoreThreads", previousCore);
            restoreProperty("executionMaximumThreads", previousMaximum);
        }
    }

    private static void restoreProperty(String name, String previousValue) {
        if (previousValue == null) System.clearProperty(name);
        else System.setProperty(name, previousValue);
    }
}
