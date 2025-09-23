# DirectoryChangeListener & RecursiveVfsDirectoryWatcher

A lightweight, polling-based, **recursive** directory watcher built on Apache Commons VFS + a cron-style scheduler.
It detects **file** events (`CREATED`, `MODIFIED`, `DELETED`) under one or more roots (local `Path` or remote via VFS),
with optional **debouncing** and **rate limiting** to tame event storms.

> Callbacks should be **fast and non-blocking**. Heavy work belongs on your own executor.

---

## DirectoryChangeListener

The callback interface the watcher uses to deliver events.

### Methods

- `void onFileCreated(Path path)` — a new regular file appeared.
- `void onFileModified(Path path)` — an existing file changed (`size` and/or `lastModified` differ).
- `void onFileDeleted(Path path)` — a previously seen file disappeared.
- `void onAny(Path path)` — fired **after** the specific callback for auditing, metrics, etc.
- `void onFail(Throwable t)` — non-fatal error while scanning or dispatching (I/O/VFS hiccups, listener exceptions, etc.).

> **Tip (tests & CI):** When mutating a file, change its **size** (e.g., append bytes) to avoid relying solely on millisecond mtime resolution.

### Example

```java
DirectoryChangeListener listener = new DirectoryChangeListener() {
    @Override public void onFileCreated(Path p)  { System.out.println("CREATE " + p); }
    @Override public void onFileModified(Path p) { System.out.println("MODIFY " + p); }
    @Override public void onFileDeleted(Path p)  { System.out.println("DELETE " + p); }
    @Override public void onAny(Path p)          { /* metrics / audit */ }
    @Override public void onFail(Throwable t)    { t.printStackTrace(); }
};
```

---

## RecursiveVfsDirectoryWatcher

`RecursiveVfsDirectoryWatcher` walks the directory tree on a fixed schedule (seconds-level cron) and diffs snapshots
to detect **file** changes.

### Key features

- **Multiple roots** (local `Path` or VFS URI) — each with its own settings and listener.
- **Global + per-root listener** (`onAny`) for centralized metrics.
- **Debounce** windows — global per-root and **matcher-specific** (`glob:` / `regex:`).
- **Rate limits** — global per-root and **matcher-specific** (events/second).
- **Per-root exclusions** (local paths).
- **Cron-driven** polling (e.g., `*/N` seconds); debounce & rate counters tick at 1s via the same scheduler.
- Optional **JVM shutdown hook** for automatic `close()`.

### Quick Start

```java
RecursiveVfsDirectoryWatcher watcher =
    new RecursiveVfsDirectoryWatcher.Builder()
        // Choose one root starter:
        .rootPath(Paths.get("/var/data"))              // local path (Path)
        // .rootPath("/var/data")                      // local path (String)
        // .rootUri("file:///var/data")                // explicit VFS URI
        // .rootUri("sftp://user@host:22/inbox/")      // remote via VFS (provider must be on classpath)
        .listener(new DirectoryChangeListener() {
            @Override public void onFileCreated(Path p)  { /* handle */ }
            @Override public void onFileModified(Path p) { /* handle */ }
            @Override public void onFileDeleted(Path p)  { /* handle */ }
            @Override public void onAny(Path p)          { /* metrics */ }
            @Override public void onFail(Throwable t)    { /* log */ }
        })
        .pollingIntervalMillis(500)                     // dev/test: faster reaction (rounded up to 1s)
        .globalDebounceWindowMillis(200)                // coalesce bursts per path
        .matcherDebounceWindow("glob:**/*.tmp", 2000)   // noisy tmp files: hold off
        .globalRateLimitPerSecond(200)                  // bound total throughput
        .matcherRateLimit("glob:**/*.log", 5)           // at most 5 events/sec for *.log
        .excludePath(Paths.get("/var/data/cache"))      // skip heavy subtree (local only)
        .shutdownHookEnabled(true)
        .build();

watcher.start();
// ...
watcher.close();
```

### Multi-root example

```java
RecursiveVfsDirectoryWatcher watcher =
    new RecursiveVfsDirectoryWatcher.Builder()
        .scheduler(customCron) // optional; will create a default CronScheduler if omitted
        .globalListener(metricsListener) // receives onAny() for every root
        .rootPath("/data/inbox")
            .listener(inboxListener)
            .pollingIntervalMillis(1000)
            .matcherDebounceWindow("glob:**/*.part", 1500)
        .rootUri("sftp://etl@host:22/drop/")
            .listener(remoteListener)
            .pollingCron("*/5 * * * * *") // every 5 seconds
            .globalRateLimitPerSecond(50)
        .build();
watcher.start();
```

### Event semantics

- A file is considered **modified** if `(size, lastModified)` differs between consecutive polls.
- Only **regular files** are tracked; directories are not reported as events.
- Coalescing model (per path):
  - `CREATED` then later `MODIFIED` → still **CREATED** (until emitted).
  - Anything with `DELETED` → **DELETED** wins.
- After delivering the specific event, the watcher calls `onAny(path)` for both per-root and global listeners (if present).

### Scheduling & resolution

- Polling uses a cron expression with **seconds** field (e.g., `*/3 * * * * *`).
- If you set `pollingIntervalMillis(x)`:
  - values `< 1000ms` are **rounded up to 1s**,
  - otherwise the interval is converted to `*/N` seconds.
- You can override the interval with `pollingCron(...)` (takes precedence).
- Internally the watcher also schedules:
  - a `*/1` second **debounce emit tick**,
  - a `*/1` second **rate-counter reset** tick.

### Debounce

- `globalDebounceWindowMillis(ms)` delays an event until `ms` of silence have passed for the **same path**.
- `matcherDebounceWindow(pattern, ms)` applies a different window for matching paths.
- Patterns use:
  - `glob:` (via `FileSystems.getDefault().getPathMatcher(...)`),
  - `regex:` (compiled `Pattern`),
  - no prefix → treated as `glob:`.

### Rate limiting

- `globalRateLimitPerSecond(n)` caps total emitted events per root per second (≤ 0 disables).
- `matcherRateLimit(pattern, n)` caps events per second for matching paths.
- Non-matching paths are unaffected by matcher caps.
- Counters reset every second by the scheduled tick.

### Exclusions

- `excludePath(path)` skips a **local** subtree (`absPath.startsWith(excluded)`).
- For VFS roots, an approximate local-style comparison is used by normalizing the VFS name path; exclude lists are still specified as local `Path` entries.

### Lifecycle

- `start()` is idempotent; throws if the watcher has been `close()`d.
- `close()` cancels all scheduled jobs and closes the VFS manager (if used).
- If `shutdownHookEnabled(true)`, a JVM shutdown hook will call `close()`.

### Error handling

- Unexpected exceptions during scanning or listener dispatch are logged and forwarded to
  `DirectoryChangeListener.onFail(Throwable)`. Keep your handler robust.

### Testing tips

- When asserting **MODIFIED**, change file **size** (e.g., write `"v22"` after `"v1"`) to make the diff independent of
  OS mtime rounding.
- In deterministic tests, use a **fake scheduler** to manually trigger: poll → emit → rate reset.

### Limitations

- Polling can miss very short-lived create/delete flaps between scans.
- Change detection relies on `(size, lastModified)`; exotic filesystems that don’t update these may be inaccurate.
- Pattern matching uses the **default JVM filesystem** semantics, which can differ from the remote VFS provider.

---

## Reference: Builder options

Per **watcher**:

- `.scheduler(CronSchedulerInterface scheduler)` — provide your own scheduler (optional).
- `.shutdownHookEnabled(boolean enabled)` — install a JVM shutdown hook (default: `false`).
- `.globalListener(DirectoryChangeListener l)` — receive `onAny(...)` for all roots (optional).

Per **root** (call after a root starter):

- Root starters (choose one per root):  
  `.rootPath(Path p)` | `.rootPath(String p)` | `.rootUri(String vfsUri)`

- Required:
  - `.listener(DirectoryChangeListener l)`

- Polling:
  - `.pollingIntervalMillis(long ms)` — rounded to seconds; minimum 1s.
  - `.pollingCron(String cron)` — e.g., `"*/2 * * * * *"`; **overrides** interval.

- Debounce:
  - `.globalDebounceWindowMillis(long ms)`
  - `.matcherDebounceWindow(String pattern, long ms)` — `glob:` / `regex:`; no prefix → `glob:`.

- Rate limiting:
  - `.globalRateLimitPerSecond(int n)` — `<= 0` disables.
  - `.matcherRateLimit(String pattern, int n)`

- Exclusions:
  - `.excludePath(Path excluded)` — local subtree only.

---

## Compatibility & notes

- Works with Apache Commons VFS; remote schemes (e.g., `sftp:`) need the corresponding provider on the classpath.
- For VFS roots the watcher initializes a `StandardFileSystemManager` internally and ensures the root exists.

