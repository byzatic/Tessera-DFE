package io.github.byzatic.tessera.engine.domain.business.common;

import io.github.byzatic.commons.schedulers.cron.CronScheduler;
import io.github.byzatic.commons.schedulers.cron.CronSchedulerInterface;
import io.github.byzatic.commons.schedulers.cron.CronTask;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * Multi-root recursive directory watcher powered by CronScheduler.
 *
 * <p>Key features:
 * <ul>
 *   <li>Multiple roots (local or VFS URIs) with per-root configuration</li>
 *   <li>Global and per-root listeners</li>
 *   <li>Debounce windows: global per-root and per-matcher (glob:/regex:)</li>
 *   <li>Rate limits: global per-root and per-matcher (events/sec)</li>
 *   <li>Exclusion subtrees per-root</li>
 *   <li>Polling via CronScheduler ({@code *}{@code /}{@code N}), debounce/rate ticks also via CronScheduler</li>
 *   <li>Optional JVM shutdown hook</li>
 * </ul>
 *
 * <p><b>Resolution:</b> Cron-based scheduling is in seconds. Sub-second millis are rounded up to 1s.</p>
 */
public class RecursiveVfsDirectoryWatcher implements AutoCloseable, Closeable {

    private static final Logger log = LoggerFactory.getLogger(RecursiveVfsDirectoryWatcher.class);

    // ---- Cron components ----
    private final CronSchedulerInterface scheduler;
    private final boolean shutdownHookEnabled;
    private Thread shutdownHook;

    // ---- Configuration ----
    private final List<RootConfig> roots;
    private final DirectoryChangeListener globalListener;

    // ---- Lifecycle ----
    private volatile boolean started = false;
    private volatile boolean closed = false;

    // ===== Builder =====
    public static class Builder {

        // global/defaults
        private CronSchedulerInterface scheduler;
        private boolean shutdownHookEnabled = false;
        private DirectoryChangeListener globalListener;

        // multiroot
        private final List<RootConfig> roots = new ArrayList<>();
        private RootConfig current; // last defined root to apply subsequent setters

        /** Provide an existing CronScheduler implementation. */
        public Builder scheduler(CronSchedulerInterface scheduler) {
            this.scheduler = requireNonNull(scheduler, "scheduler");
            return this;
        }

        /** Enable/disable JVM shutdown hook (default: false). */
        public Builder shutdownHookEnabled(boolean enabled) {
            this.shutdownHookEnabled = enabled;
            return this;
        }

        /** Set a global listener that receives events from all roots in addition to per-root listeners. */
        public Builder globalListener(DirectoryChangeListener listener) {
            this.globalListener = listener;
            return this;
        }

        // ---------- Root starters (each call creates a new root config) ----------

        /** Start configuring a new root using local Path. */
        public Builder rootPath(Path path) {
            requireNonNull(path, "path");
            return newRoot(RootConfig.forLocal(path));
        }

        /** Start configuring a new root using local path string. */
        public Builder rootPath(String path) {
            requireNonNull(path, "path");
            return newRoot(RootConfig.forLocal(Paths.get(path)));
        }

        /** Start configuring a new root using explicit VFS URI (e.g. file:///..., sftp://...). */
        public Builder rootUri(String uri) {
            requireNonNull(uri, "uri");
            return newRoot(RootConfig.forUri(URI.create(uri)));
        }

        private Builder newRoot(RootConfig rc) {
            this.roots.add(rc);
            this.current = rc;
            return this;
        }

        // ---------- Per-root configuration (applies to the most recent root) ----------

        /** Set per-root listener. */
        public Builder listener(DirectoryChangeListener listener) {
            ensureRoot();
            current.listener = requireNonNull(listener, "listener");
            return this;
        }

        /**
         * Set polling interval in milliseconds. Sub-second values are rounded up to 1s.
         * Mutually compatible with {@link #pollingCron(String)}; cron takes precedence if provided.
         */
        public Builder pollingIntervalMillis(long millis) {
            ensureRoot();
            if (millis <= 0) throw new IllegalArgumentException("pollingIntervalMillis must be > 0");
            current.pollingIntervalMillis = millis;
            return this;
        }

        //        /** Provide explicit cron (with seconds field) for polling, e.g. "*/2 * * * * *" -> every 2s. */
        public Builder pollingCron(String cron) {
            ensureRoot();
            current.pollingCron = requireNonNull(cron, "cron");
            return this;
        }

        /** Set a global debounce window (milliseconds) for this root. */
        public Builder globalDebounceWindowMillis(long millis) {
            ensureRoot();
            if (millis < 0) throw new IllegalArgumentException("debounce millis must be >= 0");
            current.globalDebounceMillis = millis;
            return this;
        }

        /** Add a matcher-specific debounce window (milliseconds). Pattern supports "glob:" or "regex:". */
        public Builder matcherDebounceWindow(String pattern, long millis) {
            ensureRoot();
            if (millis < 0) throw new IllegalArgumentException("debounce millis must be >= 0");
            current.matcherDebounceMillis.put(new MatcherSpec(pattern), millis);
            return this;
        }

        /** Set a global rate-limit in events/second for this root. 0 or negative -> unlimited. */
        public Builder globalRateLimitPerSecond(int eventsPerSec) {
            ensureRoot();
            current.globalRateLimitPerSec = eventsPerSec;
            return this;
        }

        /** Add a matcher-specific rate-limit in events/second. Pattern supports "glob:" or "regex:". */
        public Builder matcherRateLimit(String pattern, int eventsPerSec) {
            ensureRoot();
            current.matcherRateLimitPerSec.put(new MatcherSpec(pattern), eventsPerSec);
            return this;
        }

        /** Exclude a subtree from scanning (local paths only). */
        public Builder excludePath(Path exclude) {
            ensureRoot();
            current.excluded.add(exclude.toAbsolutePath().normalize());
            return this;
        }

        private void ensureRoot() {
            if (current == null) throw new IllegalStateException("Call rootPath(...) or rootUri(...) first");
        }

        public RecursiveVfsDirectoryWatcher build() {
            if (scheduler == null) {
                // your CronScheduler has only Builder-based construction
                scheduler = new CronScheduler.Builder().build();
            }
            if (roots.isEmpty()) throw new IllegalStateException("At least one root must be configured");
            for (RootConfig rc : roots) {
                if (rc.listener == null) {
                    throw new IllegalStateException("Root " + rc.describe() + " has no listener set");
                }
            }
            return new RecursiveVfsDirectoryWatcher(scheduler, roots, globalListener, shutdownHookEnabled);
        }
    }

    // ===== Implementation =====

    private RecursiveVfsDirectoryWatcher(CronSchedulerInterface scheduler,
                                         List<RootConfig> roots,
                                         DirectoryChangeListener globalListener,
                                         boolean shutdownHookEnabled) {
        this.scheduler = requireNonNull(scheduler, "scheduler");
        this.roots = List.copyOf(roots);
        this.globalListener = globalListener;
        this.shutdownHookEnabled = shutdownHookEnabled;

        // Bind global listener to each root (safe for null)
        for (RootConfig rc : this.roots) {
            rc.attachGlobal(globalListener);
        }
    }

    /** Starts polling, debounce and rate-limit ticks for all roots using CronScheduler. Idempotent. */
    public synchronized void start() {
        if (started) return;
        if (closed) throw new IllegalStateException("Watcher already closed");
        log.debug("Starting RecursiveVfsDirectoryWatcher with {} root(s)", roots.size());

        for (RootConfig rc : roots) {
            rc.initializeBackingFS(); // set up VFS handle if needed
            rc.start(this.scheduler);
        }

        if (shutdownHookEnabled && shutdownHook == null) {
            shutdownHook = new Thread(() -> {
                try {
                    log.debug("Shutdown hook: closing watcher");
                    RecursiveVfsDirectoryWatcher.this.close();
                } catch (Exception e) {
                    log.error("Error during shutdown close", e);
                }
            }, "recursive-vfs-watcher-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        started = true;
    }

    /** Closes all roots and cancels cron tasks. Safe to call multiple times. */
    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;

        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignore) {
                // JVM is already shutting down
            }
        }

        for (RootConfig rc : roots) {
            try {
                rc.close();
            } catch (Exception e) {
                log.error("Error closing root {}", rc.describe(), e);
                safeOnFail(rc.listener, e);
                if (globalListener != null) safeOnFail(globalListener, e);
            }
        }
        log.debug("RecursiveVfsDirectoryWatcher closed");
    }

    // ===== RootConfig =====

    private static final class RootConfig implements Closeable {

        // identity
        private final boolean isLocal;
        private final Path localRoot;  // if isLocal
        private final URI vfsUri;      // if !isLocal

        // runtime FS
        private FileObject vfsRoot;    // lazily created for VFS
        private FileSystemManager vfsMgr;

        // per-root config
        private DirectoryChangeListener listener;
        private DirectoryChangeListener globalListenerRef; // bound from outer
        private long pollingIntervalMillis = 1000L;
        private String pollingCron = null; // overrides interval when present
        private long globalDebounceMillis = 0L;
        private final Map<MatcherSpec, Long> matcherDebounceMillis = new LinkedHashMap<>();
        private int globalRateLimitPerSec = 0;
        private final Map<MatcherSpec, Integer> matcherRateLimitPerSec = new LinkedHashMap<>();
        private final Set<Path> excluded = new LinkedHashSet<>();

        // state: snapshots & buffers
        private final Map<String, FileStamp> snapshot = new ConcurrentHashMap<>();
        private final Map<String, PendingEvent> pending = new ConcurrentHashMap<>();
        private final Queue<PendingEvent> emitQueue = new ConcurrentLinkedQueue<>();

        // rate counters (refreshed each second by cron)
        private volatile int emittedThisSecond = 0;
        private final Map<MatcherSpec, Integer> emittedThisSecondByMatcher = new ConcurrentHashMap<>();

        // cron jobs
        private UUID pollJobId;
        private UUID debounceEmitJobId;
        private UUID rateResetJobId;
        private CronSchedulerInterface schedulerRef;

        private RootConfig(Path localRoot) {
            this.isLocal = true;
            this.localRoot = localRoot.toAbsolutePath().normalize();
            this.vfsUri = null;
        }

        private RootConfig(URI vfsUri) {
            this.isLocal = false;
            this.localRoot = null;
            this.vfsUri = vfsUri;
        }

        static RootConfig forLocal(Path p) { return new RootConfig(p); }
        static RootConfig forUri(URI u) { return new RootConfig(u); }

        void attachGlobal(DirectoryChangeListener gl) { this.globalListenerRef = gl; }

        String describe() {
            return isLocal ? localRoot.toString() : vfsUri.toString();
        }

        void initializeBackingFS() {
            if (!isLocal) {
                try {
                    StandardFileSystemManager m = new StandardFileSystemManager();
                    m.init();
                    this.vfsMgr = m;
                    this.vfsRoot = m.resolveFile(vfsUri.toString());
                    if (vfsRoot == null || !vfsRoot.exists()) {
                        throw new FileSystemException("VFS root does not exist: " + vfsUri);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to init VFS for " + vfsUri, e);
                }
            } else {
                if (!Files.exists(localRoot)) {
                    throw new IllegalArgumentException("Local root does not exist: " + localRoot);
                }
            }
        }

        void start(CronSchedulerInterface scheduler) {
            this.schedulerRef = scheduler;

            // 1) Polling
            String cron = pollingCron;
            if (cron == null) {
                long sec = Math.max(1, (pollingIntervalMillis + 999) / 1000);
                if (sec > 1 && pollingIntervalMillis % 1000 != 0) {
                    log.warn("Polling interval {}ms rounded to {}s for root {}", pollingIntervalMillis, sec, describe());
                } else if (sec == 1 && pollingIntervalMillis < 1000) {
                    log.warn("Sub-second polling {}ms rounded to 1s for root {}", pollingIntervalMillis, describe());
                }
                cron = "*/" + sec + " * * * * *";
            }
            this.pollJobId = scheduler.addJob(
                    cron,
                    (CronTask) token -> safeRun(this::doPollTick),
                    /*disallowOverlap*/ true,
                    /*runImmediately*/ false
            );

            // 2) Debounce emit tick (every 1s)
            this.debounceEmitJobId = scheduler.addJob(
                    "*/1 * * * * *",
                    (CronTask) token -> safeRun(this::emitTick),
                    true,
                    false
            );

            // 3) Rate reset tick (every 1s)
            this.rateResetJobId = scheduler.addJob(
                    "*/1 * * * * *",
                    (CronTask) token -> safeRun(this::resetRateCounters),
                    true,
                    false
            );

            log.debug("Root started: {} (poll={}, debounce=1s tick, rate=1s tick)", describe(), cron);
        }

        private void safeRun(Runnable r) {
            try {
                r.run();
            } catch (Throwable t) {
                log.error("Unhandled error in root {}", describe(), t);
                if (listener != null) {
                    try { listener.onFail(t); } catch (Throwable ignored) {}
                }
            }
        }

        private void doPollTick() {
            final long ts = System.currentTimeMillis();
            final Map<String, FileStamp> now = new HashMap<>(512);

            if (isLocal) {
                if (log.isTraceEnabled()) log.trace("Poll tick (local) {}", describe());
                try {
                    Files.walkFileTree(localRoot, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (isExcluded(dir)) return FileVisitResult.SKIP_SUBTREE;
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (!attrs.isRegularFile()) return FileVisitResult.CONTINUE;
                            String key = key(file);
                            now.put(key, new FileStamp(attrs.lastModifiedTime().toMillis(), attrs.size()));
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.error("I/O during poll tick {}", describe(), e);
                    safeOnFail(listener, e);
                    return;
                }
            } else {
                if (log.isTraceEnabled()) log.trace("Poll tick (vfs) {}", describe());
                try {
                    walkVfs(vfsRoot, now);
                } catch (FileSystemException e) {
                    log.error("VFS during poll tick {}", describe(), e);
                    safeOnFail(listener, e);
                    return;
                }
            }

            // Diff snapshots
            diffAndBuffer(now, ts);
            // Swap snapshot
            snapshot.clear();
            snapshot.putAll(now);
        }

        private boolean isExcluded(Path p) {
            Path abs = p.toAbsolutePath().normalize();
            for (Path ex : excluded) {
                if (abs.startsWith(ex)) return true;
            }
            return false;
        }

        private void walkVfs(FileObject root, Map<String, FileStamp> now) throws FileSystemException {
            if (root == null) return;
            FileObject[] children = root.getChildren();
            if (children == null) return;
            for (FileObject ch : children) {
                try {
                    if (ch.getType() == FileType.FOLDER) {
                        if (isExcludedLikeLocal(ch)) continue;
                        walkVfs(ch, now);
                    } else if (ch.getType() == FileType.FILE) {
                        String key = key(ch);
                        long lm = ch.getContent().getLastModifiedTime();
                        long sz = ch.getContent().getSize();
                        now.put(key, new FileStamp(lm, sz));
                    }
                } catch (FileSystemException ex) {
                    log.debug("Skipping VFS child due to error: {}", ch, ex);
                }
            }
        }

        private boolean isExcludedLikeLocal(FileObject fo) {
            String p = fo.getName().getPath(); // normalized with '/'
            Path fake = Paths.get(p.startsWith("/") ? p : "/" + p);
            return isExcluded(fake);
        }

        private void diffAndBuffer(Map<String, FileStamp> now, long ts) {
            // created/modified
            for (Map.Entry<String, FileStamp> e : now.entrySet()) {
                String k = e.getKey();
                FileStamp cur = e.getValue();
                FileStamp prev = snapshot.get(k);
                if (prev == null) {
                    buffer(EventType.CREATED, k, ts);
                } else if (cur.isModifiedComparedTo(prev)) {
                    buffer(EventType.MODIFIED, k, ts);
                }
            }
            // deleted
            for (String k : snapshot.keySet()) {
                if (!now.containsKey(k)) {
                    buffer(EventType.DELETED, k, ts);
                }
            }
        }

        private void buffer(EventType type, String key, long ts) {
            long delay = debounceMillisFor(key);
            long readyAt = ts + delay;

            PendingEvent prev = pending.get(key);
            if (prev == null) {
                PendingEvent ne = new PendingEvent(type, key, readyAt);
                pending.put(key, ne);
                emitQueue.add(ne);
            } else {
                // coalesce
                prev.type = coalesce(prev.type, type);
                if (readyAt > prev.readyAt) prev.readyAt = readyAt;
            }

            if (log.isTraceEnabled()) {
                log.trace("Buffered {} key={} delay={}ms readyAt={}",
                        type, key, delay, Instant.ofEpochMilli(readyAt));
            }
        }

        private long debounceMillisFor(String key) {
            for (Map.Entry<MatcherSpec, Long> e : matcherDebounceMillis.entrySet()) {
                if (e.getKey().matches(key)) {
                    return e.getValue();
                }
            }
            return globalDebounceMillis;
        }

        private void emitTick() {
            if (emitQueue.isEmpty()) return;
            final long now = System.currentTimeMillis();

            int emitted = 0;
            int examined = 0;
            int limit = emitQueue.size();
            while (examined < limit) {
                PendingEvent pe = emitQueue.poll();
                examined++;
                if (pe == null) break;

                PendingEvent cur = pending.get(pe.key);
                if (cur == null || cur != pe) {
                    continue; // stale
                }
                if (cur.readyAt > now) {
                    emitQueue.add(pe);
                    continue;
                }

                if (!tryConsumeRateBudget(cur.key)) {
                    emitQueue.add(pe);
                    continue;
                }

                Path p = toPath(cur.key);
                try {
                    switch (cur.type) {
                        case CREATED -> listener.onFileCreated(p);
                        case MODIFIED -> listener.onFileModified(p);
                        case DELETED -> listener.onFileDeleted(p);
                    }
                    listener.onAny(p);
                } catch (Throwable t) {
                    log.error("Listener error for {} {}", cur.type, p, t);
                    safeOnFail(listener, t);
                }

                if (globalListenerRef != null) {
                    try {
                        globalListenerRef.onAny(p);
                    } catch (Throwable t) {
                        log.error("Global listener error for {}", p, t);
                        safeOnFail(globalListenerRef, t);
                    }
                }

                pending.remove(cur.key);
                emitted++;
            }

            if (log.isDebugEnabled() && emitted > 0) {
                log.debug("Emitted {} event(s) for root {}", emitted, describe());
            }
        }

        private boolean tryConsumeRateBudget(String key) {
            if (globalRateLimitPerSec > 0) {
                if (emittedThisSecond >= globalRateLimitPerSec) return false;
            }

            for (Map.Entry<MatcherSpec, Integer> e : matcherRateLimitPerSec.entrySet()) {
                MatcherSpec spec = e.getKey();
                int cap = e.getValue();
                if (cap <= 0) continue;
                if (spec.matches(key)) {
                    int used = emittedThisSecondByMatcher.getOrDefault(spec, 0);
                    if (used >= cap) return false;
                }
            }

            if (globalRateLimitPerSec > 0) emittedThisSecond++;
            for (Map.Entry<MatcherSpec, Integer> e : matcherRateLimitPerSec.entrySet()) {
                MatcherSpec spec = e.getKey();
                int cap = e.getValue();
                if (cap <= 0) continue;
                if (spec.matches(key)) {
                    emittedThisSecondByMatcher.merge(spec, 1, Integer::sum);
                }
            }
            return true;
        }

        private void resetRateCounters() {
            emittedThisSecond = 0;
            emittedThisSecondByMatcher.clear();
            if (log.isTraceEnabled()) log.trace("Rate counters reset for {}", describe());
        }

        private EventType coalesce(EventType oldT, EventType newT) {
            if (oldT == EventType.DELETED || newT == EventType.DELETED) return EventType.DELETED;
            if (oldT == EventType.CREATED) return EventType.CREATED;
            return newT; // MODIFIED stays modified
        }

        private String key(Path p) {
            return p.toAbsolutePath().normalize().toString();
        }

        private String key(FileObject fo) {
            String p = fo.getName().getPath();
            if (!p.startsWith("/")) p = "/" + p;
            return p;
        }

        private Path toPath(String key) {
            return Paths.get(key);
        }

        @Override
        public void close() throws IOException {
            // cancel cron jobs via scheduler
            if (schedulerRef != null) {
                if (pollJobId != null)      schedulerRef.removeJob(pollJobId);
                if (debounceEmitJobId != null) schedulerRef.removeJob(debounceEmitJobId);
                if (rateResetJobId != null) schedulerRef.removeJob(rateResetJobId);
            }

            if (vfsMgr instanceof StandardFileSystemManager m) {
                try {
                    m.close();
                } catch (Exception e) {
                    log.debug("Ignoring VFS manager close error for {}", describe(), e);
                }
            }
        }
    }

    // ===== Helpers =====

    private static void safeOnFail(DirectoryChangeListener l, Throwable t) {
        if (l == null) return;
        try {
            l.onFail(t);
        } catch (Throwable ignore) {}
    }

    private enum EventType { CREATED, MODIFIED, DELETED }

    private static record FileStamp(long lastModified, long size) {
        boolean isModifiedComparedTo(FileStamp other) {
            return this.lastModified != other.lastModified || this.size != other.size;
        }
    }

    private static final class PendingEvent {
        volatile EventType type;
        final String key;
        volatile long readyAt;

        PendingEvent(EventType type, String key, long readyAt) {
            this.type = type;
            this.key = key;
            this.readyAt = readyAt;
        }
    }

    /** Glob/regex matcher that works on normalized string keys. */
    private static final class MatcherSpec {
        private final String raw;
        private final String mode; // glob|regex
        private final PathMatcher nioMatcher; // for glob/regex via default FS
        private final java.util.regex.Pattern regex; // compiled when mode==regex

        MatcherSpec(String spec) {
            requireNonNull(spec, "spec");
            this.raw = spec;
            if (spec.startsWith("glob:")) {
                this.mode = "glob";
                this.regex = null;
                this.nioMatcher = FileSystems.getDefault().getPathMatcher(spec);
            } else if (spec.startsWith("regex:")) {
                this.mode = "regex";
                this.nioMatcher = null;
                this.regex = java.util.regex.Pattern.compile(spec.substring("regex:".length()));
            } else {
                this.mode = "glob";
                this.regex = null;
                this.nioMatcher = FileSystems.getDefault().getPathMatcher("glob:" + spec);
            }
        }

        boolean matches(String key) {
            if ("glob".equals(mode)) {
                Path p = Paths.get(key);
                return nioMatcher.matches(p);
            } else {
                return regex.matcher(key).matches();
            }
        }

        @Override public String toString() { return raw; }
        @Override public int hashCode() { return raw.hashCode(); }
        @Override public boolean equals(Object o) {
            return (o instanceof MatcherSpec m) && this.raw.equals(m.raw);
        }
    }
}