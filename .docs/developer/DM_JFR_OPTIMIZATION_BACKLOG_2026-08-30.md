# DM: JFR optimization backlog after eager-exception fix

- Status: Proposed
- Date: 2026-08-30
- Project: Tessera-DFE 0.1.8-SNAPSHOT
- Source recording: `flight_recording/recording.jfr`
- Recording start: 2026-08-30 19:42:22 UTC
- Duration: 300 seconds
- Runtime: OpenJDK 17.0.20, Linux, 8 hardware threads, `-Xms512m -Xmx3072m`, G1
- Purpose: preserve the evidence and task definitions needed for the next optimization cycle

## 1. Context and decision basis

The previous recording was dominated by exception construction in `ExecutionContext`:

- `ExecutionContext.<init>`: 57.52% of execution samples;
- `ExecutionContext.Builder.build`: 10.61%;
- approximately 10.1 million `Throwable` instances during the recording.

Tessera-DFE was changed so that validation exceptions are created only when a value is actually invalid. Incorrect `StorageDescription` getters were fixed at the same time.

The new recording confirms that this work removed the original bottleneck:

| Metric | Previous recording | New recording |
|---|---:|---:|
| Execution samples | 10,234 | 2,280 |
| Average JVM user CPU | 10.35% | 6.78% |
| Average JVM system CPU | 1.05% | 0.86% |
| `ExecutionContext` constructor and builder | 68.13% | absent from hot methods |
| Allocated by threads | approximately 107 GB | approximately 323 GB |
| Throwable count delta | 10,112,483 | 12,447,750 |
| GC pauses | 161 | 350 |
| Total GC pause time | 650 ms | 870 ms |
| GC P99 | 30.3 ms | 8.38 ms |
| Maximum GC pause | 36.7 ms | 17.0 ms |
| Active platform threads, first to last | 42 to 42 | 41 to 41 |

Interpretation:

- The original CPU bottleneck is resolved.
- Lower CPU sampling density and lower average JVM CPU indicate a real improvement.
- Higher allocation volume and more frequent GC are consistent with higher completed-work throughput after removing the old bottleneck, but throughput was not recorded explicitly. Allocation volume must therefore not be interpreted as a regression without a business throughput denominator.
- GC frequency increased, but tail pause latency improved substantially.
- Heap after GC ended around 827 MB, close to the previous 812 MB. Active thread count was stable and no monitor contention was recorded. The recording does not prove a memory or thread leak.
- `ObjectAllocationSample.weight` is sampled allocation pressure, not the literal size of the displayed object. It is suitable for ranking candidates, not for attributing exact byte totals to one class.

## 2. Task A: remove eager exceptions from workflow toolkit libraries

### How the need was established

The DFE-local eager exception sources disappeared, but the new recording still contains a delta of 12,447,750 `Throwable` objects. Allocation samples identify `IllegalArgumentException` construction in:

- `io.github.byzatic.tessera.workflowroutine.workflowroutines.AbstractWorkflowRoutine`;
- `io.github.byzatic.tessera.storageapi.dto.StorageItem`;
- several workflow processors when resolving missing arguments.

Bytecode inspection of `tessera-workflowroutine-lib-0.0.1.jar` confirms that `AbstractWorkflowRoutine` constructs an exception before every `ObjectsUtils.requireNonNull` call. Its main constructor performs approximately eight such validations. Some getters and state methods use the same pattern with `IllegalStateException`.

Bytecode inspection of `tessera-storageapi-lib-0.0.1.jar` confirms the same construction pattern in `StorageItem`.

These libraries are consumed through `version.tessera_workflow_toolkit=0.0.1`. Their sources are not part of the Tessera-DFE repository, so this task must be implemented in the corresponding toolkit repositories and then consumed through an updated dependency version.

### Task statement

Change all toolkit validation paths so that exception objects and their messages are created only when the validated condition fails. Preserve the public exception type and message for invalid input.

The audit must cover at least:

- `tessera-workflowroutine-lib`, especially `AbstractWorkflowRoutine` constructors and accessors;
- `tessera-storageapi-lib`, especially `StorageItem` and other frequently built DTOs;
- `tessera-service-lib` and `tessera-enginecommon-lib` for the same `ObjectsUtils.requireNonNull(value, new Exception(...))` pattern;
- workflow module code that uses exceptions as normal argument lookup or parser control flow.

### Implementation constraints

- Do not replace `IllegalArgumentException` or `IllegalStateException` with a different externally visible type unless an API change is explicitly approved.
- Prefer a direct conditional check in hot code. A lazy supplier helper is acceptable only if it does not allocate a capturing lambda on every successful call.
- Do not suppress stack traces globally. These exceptions must remain diagnostically useful when a real validation failure occurs.

### Acceptance criteria

- No eager `new ...Exception(...)` argument remains in production `requireNonNull` calls across the toolkit modules.
- Existing invalid-input tests continue to verify exception type and message.
- New tests cover the principal `AbstractWorkflowRoutine` and `StorageItem` validation contracts.
- A comparable 300-second JFR recording no longer attributes meaningful `IllegalArgumentException` allocation pressure to toolkit constructors.
- The `ExceptionStatistics` delta is reported both as an absolute count and per completed pipeline or routine execution.

## 3. Task B: cache DSL loading and parsing per project revision

### How the need was established

After removal of the old hotspot, ANTLR became the dominant CPU consumer:

- `LexerATNSimulator.execATN`: 20.31% of execution samples;
- additional samples in `Lexer.nextToken`, `Parser.match`, `BufferedTokenStream`, `IntervalSet`, `CodePointBuffer`, and parse-tree operations;
- major allocation pressure in `CharStreams.fromString`, `CommonToken`, `TerminalNodeImpl`, `ParserRuleContext`, character buffers, byte buffers, strings, and arrays.

Application stacks show repeated parsing from:

- data-enrichment processors;
- processing-status processors;
- graph-lifting-data processors;
- get-data configuration loading through `ConfigLoader.readConfig` and Gson.

The configuration and DSL files belong to a project revision and normally remain unchanged during repeated executions of that revision. Re-reading and parsing them for each routine execution is therefore duplicated work.

### Task statement

Introduce a revision-scoped cache of loaded configuration and compiled DSL representations. Repeated routine executions within the same project revision must reuse immutable parsed data instead of reading files and rebuilding ANTLR lexer, token stream, parser, parse tree, and Gson object graphs.

### Required design decisions

Before implementation, define:

- the cache key: canonical file path plus project revision identity is preferred;
- the cached value: immutable semantic command model is preferred over a mutable ANTLR parse tree;
- ownership: the project runtime session should own the cache so that project shutdown or reload deterministically releases it;
- invalidation: a new revision must receive a new cache; the old cache must not observe files from the new revision;
- concurrency: concurrent workers parsing the same file must share one completed value without duplicating expensive work;
- failure behavior: parsing failures must not poison later retries indefinitely unless the revision itself is rejected.

### Non-goals

- Do not create a process-global cache keyed only by path. Different project revisions may reuse paths with different content or class loaders.
- Do not retain plugin class loaders after their project runtime has stopped.
- Do not share mutable parser, listener, or DTO state between routine invocations.

### Acceptance criteria

- Each unchanged DSL or JSON configuration file is read and parsed at most once per project revision under concurrent access.
- Reloading to a new revision cannot return data parsed for the previous revision.
- Cache lifecycle and class-loader release are covered by tests.
- ANTLR methods no longer dominate `hot-methods` in a comparable recording.
- Allocation pressure from ANTLR tokens, parse-tree nodes, character buffers, and `ConfigLoader.readConfig` decreases materially per completed routine.

## 4. Task C: precompute worker configuration parameters and resolved paths

### How the need was established

`SupportPathResolver.resolvePath` accounts for 5.66% of new execution samples. The absolute sample count remained almost unchanged: 126 in the previous recording and 129 in the new recording. The earlier local cleanup reduced incidental copying but did not remove repeated work.

`PipelineManager.runPipeline` currently rebuilds a `List<ConfigurationParameter>` for every worker execution and calls `pathResolver.processTemplate` for every configured file. Node and project-global base paths do not change during the lifetime of a project revision.

### Task statement

Precompute immutable worker configuration parameters, including resolved node and project-global paths, once for the lifetime of the relevant `PipelineManager` or project revision. `runPipeline` must reuse the prepared values.

### Implementation constraints

- Preserve the exact existing path semantics, including normalization and handling of `${NODE_PATH}` and `${PROJECT_GLOBAL_PATH}`.
- Do not reuse configuration across nodes when `${NODE_PATH}` differs.
- Store immutable lists or defensive copies so routines cannot mutate shared configuration.
- Recompute values on project revision replacement; no filesystem watcher invalidation is required within an immutable revision unless the project contract changes.

### Acceptance criteria

- `runPipeline` performs no path-template resolution for an already prepared worker.
- Unit tests cover both variables, paths without variables, leading separators, normalization, node isolation, and revision replacement.
- `SupportPathResolver` is absent from the meaningful hot-method list in a comparable JFR recording.
- Configuration values delivered to routines remain byte-for-byte equivalent to the current implementation.

## 5. Task D: cache the static portion of ExecutionContext

### How the need was established

The exception-construction problem in `ExecutionContext` is gone, exposing the remaining construction cost:

- `ExecutionContextFactory.create`: 1.54% of execution samples;
- additional samples in storage-description conversion, graph-path lookup, repository lookup, string copying, list growth, hashing, and builder creation.

For every worker invocation, `ExecutionContextFactory` currently rebuilds:

- node identity and node storage descriptions;
- global storage descriptions;
- root paths;
- the path to the current execution node;
- copied strings and intermediate lists.

Most of this data is constant for a node during one project revision. Stage, worker, execution position, and MDC values remain invocation-specific.

The public `getExecutionContext` method is also synchronized even though the factory currently has no mutable state. No contention event was captured, so synchronization removal is not justified as a standalone performance task; it should be reconsidered as part of the cache concurrency design.

### Task statement

Split execution-context construction into a revision-scoped immutable static descriptor and a small invocation-specific object. Cache the static descriptor by node and path identity, then assemble only stage, worker, execution-position, health/MDC, and other dynamic fields per invocation.

### Required design decisions

- Define whether the execution path is part of the cache key. The same graph node may be reachable through multiple paths.
- Make cached DTOs and nested collections immutable or defensively copied.
- Ensure that workflow code cannot mutate state shared with another invocation.
- Tie cache lifetime to the project runtime/revision and avoid static process-global references.
- After state is immutable and safely published, remove unnecessary synchronization only if tests demonstrate thread safety.

### Acceptance criteria

- Static storage descriptions and root paths are not rebuilt for every worker execution.
- Contexts for different workers retain correct MDC and stage data.
- Contexts for the same node but different graph paths retain the correct path-specific data.
- Concurrent context creation tests show no shared mutable-state corruption.
- `ExecutionContextFactory`, storage conversion, and graph-path construction show a material per-routine reduction in CPU samples and allocations.

## 6. Task E: reduce scheduler lifecycle allocation after upstream fixes

### How the need was established

Allocation samples attribute substantial pressure to `Instant.create` through `UnifiedScheduler.RunControl.run`. Every short-lived scheduled run creates lifecycle state including a UUID, cancellation context, atomics, futures, timestamps, and completion state. The recording also shows allocation from MDC map copying around task execution.

This is partly a throughput effect: after removal of the old CPU bottleneck, many more short-lived routines can be scheduled. Optimizing scheduler internals before eliminating repeated routine construction and parsing could target symptoms rather than the source of task churn.

### Task statement

After Tasks A-D, re-profile task creation and determine scheduler overhead per completed run. If scheduler lifecycle objects remain a material cost, reduce optional timestamp, future, event, and MDC-copy allocations without weakening cancellation, completion, timeout, or observability guarantees.

### Investigation requirements

- Add or collect completed-run throughput so allocation can be reported per run.
- Separate immediate workflow tasks from cron tasks and service tasks.
- Measure the number of `RunControl`, `Instant`, `CompletableFuture`, UUID, event, and MDC-map objects per run.
- Determine which lifecycle fields are required for every run and which are required only when queried or observed.
- Evaluate whether routine or pipeline batching can reduce scheduler submissions without changing stage ordering and failure semantics.

### Acceptance criteria

- A before/after allocation budget per completed run is documented.
- Any lazy lifecycle state remains thread-safe and safely published.
- Completion, cancellation, timeout, listener ordering, and query behavior retain regression tests.
- Scheduler changes do not reintroduce listener growth, missed terminal events, or task-registry retention.
- Optimization is accepted only if it improves allocations or CPU under equivalent throughput.

## 7. Recommended execution order

1. Task A: toolkit eager exceptions.
2. Task B: revision-scoped DSL and configuration parsing cache.
3. Task C: precomputed worker configuration parameters and paths.
4. Task D: static execution-context cache.
5. Record a new baseline with explicit throughput counters.
6. Task E only if scheduler allocation remains material per completed run.

Tasks B-D should share a single ownership model based on the project runtime revision rather than introducing independent process-global caches.

## 8. Verification protocol for the next cycle

Use the same dataset, container limits, JVM version, heap settings, JFR delay, recording duration, graph schedule, and observability settings. Record at minimum:

- completed graphs, pipelines, stages, workers, and scheduler runs;
- failed and cancelled runs;
- source records or metrics processed;
- `hot-methods` and execution sample count;
- thread allocation total and allocation per completed routine;
- `ExceptionStatistics` delta and exceptions per completed routine;
- GC count, total pause, median, P95, P99, and maximum pause;
- heap used after GC at the beginning and end of the active interval;
- active thread count at the beginning and end;
- class-loader count before startup, after warm-up, and after revision replacement.

Do not compare raw allocation totals or exception totals without the completed-work denominator. If detailed exception provenance is still needed, enable `jdk.JavaExceptionThrow` for a bounded diagnostic recording or use a profiler configuration that captures exception class and application caller without overwhelming the production run.

## 9. Completion condition for this memo

This memo can be closed when Tasks A-D have been implemented and verified under equivalent workload, and Task E has either been completed or explicitly rejected based on per-run measurements. The final recording and metric comparison should be linked here when available.
