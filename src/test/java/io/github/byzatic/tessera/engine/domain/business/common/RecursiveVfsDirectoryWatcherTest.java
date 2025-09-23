package io.github.byzatic.tessera.engine.domain.business.common;

import io.github.byzatic.commons.schedulers.cron.CronSchedulerInterface;
import io.github.byzatic.commons.schedulers.cron.CronTask;
import io.github.byzatic.commons.schedulers.cron.JobEventListener;
import io.github.byzatic.commons.schedulers.cron.JobInfo;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.*;

public class RecursiveVfsDirectoryWatcherTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private RecursiveVfsDirectoryWatcher watcher;
    private FakeScheduler scheduler;

    @After
    public void tearDown() {
        if (watcher != null) watcher.close();
    }

    @Test
    public void createdFile_emitsCreatedAndAny() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener listener = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        scheduler.runPoll();
        scheduler.runEmit();

        Path f = root.resolve("a.txt");
        Files.write(f, "hello".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        scheduler.runPoll();
        scheduler.runEmit();

        assertEquals(1, listener.created.size());
        assertEquals(f.toAbsolutePath().normalize(), listener.created.get(0));
        assertEquals(1, listener.any.size());
        assertTrue(listener.fail.isEmpty());
        assertEquals(0, listener.failNoArgCount);
    }

    @Test
    public void modifiedFile_emitsModified() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener listener = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        Path f = root.resolve("b.txt");
        Files.write(f, "v1".getBytes(StandardCharsets.UTF_8));

        // Зафиксировать исходное состояние
        scheduler.runPoll();
        scheduler.runEmit();

        // Очистить события, чтобы проверять только MODIFIED
        listener.created.clear();
        listener.modified.clear();
        listener.deleted.clear();
        listener.any.clear();

        // Меняем размер, чтобы модификация была замечена независимо от точности mtime
        Files.write(f, "v22".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

        scheduler.runPoll();
        scheduler.runEmit();

        assertEquals(1, listener.modified.size());
        assertEquals(f.toAbsolutePath().normalize(), listener.modified.get(0));
        assertTrue("после очистки не должно быть новых CREATED", listener.created.isEmpty());
        assertTrue(listener.fail.isEmpty());
        assertEquals(0, listener.failNoArgCount);
    }

    @Test
    public void deletedFile_emitsDeleted() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener listener = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        Path f = root.resolve("c.txt");
        Files.write(f, "data".getBytes(StandardCharsets.UTF_8));

        scheduler.runPoll();
        scheduler.runEmit();

        Files.delete(f);
        scheduler.runPoll();
        scheduler.runEmit();

        assertEquals(1, listener.deleted.size());
        assertEquals(f.toAbsolutePath().normalize(), listener.deleted.get(0));
        assertTrue(listener.fail.isEmpty());
        assertEquals(0, listener.failNoArgCount);
    }

    @Test
    public void coalesceBeforeEmit_createdThenModified_resultsInSingleCreated() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener listener = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        Path f = root.resolve("d.txt");

        Files.write(f, "v1".getBytes(StandardCharsets.UTF_8));
        scheduler.runPoll();     // CREATED buffered
        Files.write(f, "v2".getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
        scheduler.runPoll();     // MODIFIED buffered, coalesced => CREATED
        scheduler.runEmit();

        assertEquals(1, listener.created.size());
        assertTrue(listener.modified.isEmpty());
        assertEquals(1, listener.any.size());
        assertTrue(listener.fail.isEmpty());
    }

    @Test
    public void rateLimitGlobal_1_per_second_defersExtraEvents_untilReset() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener listener = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .globalDebounceWindowMillis(0)
                .globalRateLimitPerSecond(1)
                .build();
        watcher.start();

        Path f1 = root.resolve("e1.txt");
        Path f2 = root.resolve("e2.txt");
        Files.write(f1, "x".getBytes(StandardCharsets.UTF_8));
        Files.write(f2, "y".getBytes(StandardCharsets.UTF_8));

        scheduler.runPoll();
        scheduler.runEmit();

        assertEquals(1, listener.created.size());
        assertEquals(1, listener.any.size());

        scheduler.runRateReset();
        scheduler.runEmit();

        assertEquals(2, listener.created.size());
        Set<Path> expected = new HashSet<>(Arrays.asList(
                f1.toAbsolutePath().normalize(), f2.toAbsolutePath().normalize()));
        assertEquals(expected, new HashSet<>(listener.created));
        assertTrue(listener.fail.isEmpty());
    }

    @Test
    public void excludeSubtree_ignored() throws Exception {
        File rootDir = tmp.newFolder("root");
        Path root = rootDir.toPath();
        Path excluded = root.resolve("ignore");
        Files.createDirectories(excluded);

        CapturingListener listener = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .excludePath(excluded)
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        scheduler.runPoll();
        scheduler.runEmit();

        Path inExcl = excluded.resolve("hidden.txt");
        Files.write(inExcl, "secret".getBytes(StandardCharsets.UTF_8));
        Path visible = root.resolve("visible.txt");
        Files.write(visible, "public".getBytes(StandardCharsets.UTF_8));

        scheduler.runPoll();
        scheduler.runEmit();

        assertTrue(listener.created.contains(visible.toAbsolutePath().normalize()));
        assertFalse(listener.created.contains(inExcl.toAbsolutePath().normalize()));
    }

    @Test
    public void globalListener_receivesAnyEvents() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener perRoot = new CapturingListener();
        CapturingListener global = new CapturingListener();

        scheduler = new FakeScheduler();
        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .globalListener(global)
                .rootPath(root)
                .listener(perRoot)
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        Path f = root.resolve("g.txt");
        Files.write(f, "x".getBytes(StandardCharsets.UTF_8));

        scheduler.runPoll();
        scheduler.runEmit();

        assertEquals(1, perRoot.any.size());
        assertEquals(1, global.any.size());
        assertEquals(perRoot.any.get(0), global.any.get(0));
        assertTrue(perRoot.fail.isEmpty());
        assertTrue(global.fail.isEmpty());
    }

    @Test
    public void pollingCron_overridesInterval() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        scheduler = new FakeScheduler();

        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(new CapturingListener())
                .pollingIntervalMillis(10_000)       // should be ignored
                .pollingCron("*/3 * * * * *")        // should take precedence
                .build();
        watcher.start();

        assertEquals("*/3 * * * * *", scheduler.getCron(0));
    }

    @Test
    public void pollingInterval_subSecondRoundedTo1s() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        scheduler = new FakeScheduler();

        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(new CapturingListener())
                .pollingIntervalMillis(250)          // < 1s => round up to 1s
                .build();
        watcher.start();

        assertEquals("*/1 * * * * *", scheduler.getCron(0));
    }

    @Test
    public void matcherRateLimit_appliesOnlyToMatchingPattern() throws Exception {
        Path root = tmp.newFolder("root").toPath();
        CapturingListener listener = new CapturingListener();
        scheduler = new FakeScheduler();

        watcher = new RecursiveVfsDirectoryWatcher.Builder()
                .scheduler(scheduler)
                .rootPath(root)
                .listener(listener)
                .matcherRateLimit("glob:**/*.log", 1)   // only *.log capped to 1 per second
                .globalDebounceWindowMillis(0)
                .build();
        watcher.start();

        Path aLog = root.resolve("a.log");
        Path bLog = root.resolve("b.log");
        Path txt  = root.resolve("c.txt");
        Files.write(aLog, "a".getBytes(StandardCharsets.UTF_8));
        Files.write(bLog, "b".getBytes(StandardCharsets.UTF_8));
        Files.write(txt,  "c".getBytes(StandardCharsets.UTF_8));

        scheduler.runPoll();
        scheduler.runEmit();

        // First window: one of the *.log + the txt (uncapped) => 2 events total
        assertEquals(2, listener.created.size());

        scheduler.runRateReset();
        scheduler.runEmit();

        // Second window: remaining *.log is emitted => total 3
        assertEquals(3, listener.created.size());
        Set<Path> expected = new HashSet<>(Arrays.asList(
                aLog.toAbsolutePath().normalize(),
                bLog.toAbsolutePath().normalize(),
                txt.toAbsolutePath().normalize()
        ));
        assertEquals(expected, new HashSet<>(listener.created));
    }

    // ----------------- Helpers -----------------

    static class CapturingListener implements DirectoryChangeListener {
        final List<Path> created = new CopyOnWriteArrayList<>();
        final List<Path> modified = new CopyOnWriteArrayList<>();
        final List<Path> deleted = new CopyOnWriteArrayList<>();
        final List<Path> any = new CopyOnWriteArrayList<>();
        final List<Throwable> fail = new CopyOnWriteArrayList<>();
        volatile int failNoArgCount = 0;

        @Override public void onFileCreated(Path p) { created.add(p.toAbsolutePath().normalize()); }
        @Override public void onFileModified(Path p) { modified.add(p.toAbsolutePath().normalize()); }
        @Override public void onFileDeleted(Path p) { deleted.add(p.toAbsolutePath().normalize()); }
        @Override public void onAny(Path p)         { any.add(p.toAbsolutePath().normalize()); }

        // На случай, если интерфейс имеет версию без аргументов
        public void onFail() { failNoArgCount++; }

        @Override public void onFail(Throwable t) { fail.add(t); }
    }

    static class FakeScheduler implements CronSchedulerInterface {

        static class Job {
            final UUID id;
            final String cron;
            final CronTask task;
            final boolean disallowOverlap;
            final boolean runImmediately;
            Job(UUID id, String cron, CronTask task, boolean disallowOverlap, boolean runImmediately) {
                this.id = id; this.cron = cron; this.task = task;
                this.disallowOverlap = disallowOverlap; this.runImmediately = runImmediately;
            }
        }

        private final List<Job> jobs = new ArrayList<>();
        private final List<JobEventListener> listeners = new ArrayList<>();

        // ---- Introspection for tests ----
        String getCron(int index) {
            if (index < 0 || index >= jobs.size()) throw new IndexOutOfBoundsException();
            return jobs.get(index).cron;
        }

        // ---- CronSchedulerInterface impl ----

        @Override
        public void addListener(JobEventListener l) { if (l != null) listeners.add(l); }

        @Override
        public void removeListener(JobEventListener l) { listeners.remove(l); }

        @Override
        public UUID addJob(String cron, CronTask task, boolean disallowOverlap, boolean runImmediately) {
            UUID id = UUID.randomUUID();
            Job j = new Job(id, cron, task, disallowOverlap, runImmediately);
            jobs.add(j);
            if (runImmediately) {
                try { task.run(null); } catch (Exception e) { throw new RuntimeException(e); }
            }
            return id;
        }

        @Override
        public UUID addJob(String cron, CronTask task) {
            return addJob(cron, task, false, false);
        }

        @Override
        public UUID addJob(String cron, CronTask task, boolean disallowOverlap) {
            return addJob(cron, task, disallowOverlap, false);
        }

        @Override
        public boolean removeJob(UUID jobId) { return jobs.removeIf(j -> j.id.equals(jobId)); }

        @Override
        public boolean removeJob(UUID jobId, Duration grace) { return removeJob(jobId); }

        @Override
        public void stopJob(UUID jobId, Duration grace) { removeJob(jobId); }

        @Override
        public Optional<JobInfo> query(UUID jobId) { return Optional.empty(); }

        @Override
        public List<JobInfo> listJobs() { return Collections.emptyList(); }

        @Override
        public void close() { jobs.clear(); listeners.clear(); }

        // --- Drivers ---

        void runPoll() { runJob(0); }

        void runEmit() { runJob(1); }

        void runRateReset() { runJob(2); }

        private void runJob(int idx) {
            if (jobs.size() <= idx) throw new IllegalStateException("Scheduler has " + jobs.size() + " job(s), need ≥ " + (idx+1));
            try { jobs.get(idx).task.run(null); } catch (Exception e) { throw new RuntimeException(e); }
        }
    }
}