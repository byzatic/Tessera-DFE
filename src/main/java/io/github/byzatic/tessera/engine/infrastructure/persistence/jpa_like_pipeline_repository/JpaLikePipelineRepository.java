package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_pipeline_repository;

import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_pipeline.NodePipeline;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikePipelineRepositoryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe repository that loads and serves {@link NodePipeline} objects by {@link GraphNodeRef}.
 * <p>
 * Loading/reloading is coordinated via a {@link CountDownLatch}. Readers wait (with a timeout)
 * until the current load cycle completes to avoid observing partially initialized state.
 * The repository also exposes an {@code isLoaded} flag to short-circuit waiting when data is known
 * to be fully published.
 * </p>
 */
public class JpaLikePipelineRepository implements JpaLikePipelineRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(JpaLikePipelineRepository.class);

    /** Max time (in seconds) a reader waits for the repository to complete loading. */
    private final long waitTimeoutSeconds;

    private final PipelineDaoInterface pipelineDao;
    private final String projectName;

    /** Storage published-before countDown() in reload(), then read after await(). */
    private final Map<GraphNodeRef, NodePipeline> nodePipelineMap = new HashMap<>();

    /** Load-state and synchronization primitives for load/reload cycles. */
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private volatile CountDownLatch loadLatch = new CountDownLatch(1);
    private final Object reloadLock = new Object();

    /**
     * Creates a repository with a default 10-second wait timeout for readers.
     *
     * @param projectName non-null project name
     * @param pipelineDao non-null DAO used to load pipelines
     * @throws RuntimeException if arguments are invalid
     */
    public JpaLikePipelineRepository(String projectName, PipelineDaoInterface pipelineDao)
            throws RuntimeException {
        this(projectName, pipelineDao, 10L);
    }

    /**
     * Creates a repository with a custom wait timeout for readers.
     *
     * @param projectName        non-null project name
     * @param pipelineDao        non-null DAO used to load pipelines
     * @param waitTimeoutSeconds non-null timeout in seconds readers will wait for loading to finish
     * @throws RuntimeException if arguments are invalid
     */
    public JpaLikePipelineRepository(String projectName, PipelineDaoInterface pipelineDao, Long waitTimeoutSeconds)
            throws RuntimeException {
        try {
            ObjectsUtils.requireNonNull(projectName, new IllegalArgumentException("ProjectName should not be null"));
            ObjectsUtils.requireNonNull(pipelineDao, new IllegalArgumentException("PipelineDao should not be null"));
            ObjectsUtils.requireNonNull(waitTimeoutSeconds, new IllegalArgumentException("waitTimeoutSeconds should not be null"));
            this.projectName = projectName;
            this.pipelineDao = pipelineDao;
            this.waitTimeoutSeconds = waitTimeoutSeconds;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Returns a pipeline for the given node reference. This method blocks (up to the configured timeout)
     * until the repository has finished its most recent load/reload cycle.
     *
     * @param graphNodeRef node reference key
     * @return non-null {@link NodePipeline}
     * @throws OperationIncompleteException if the pipeline is absent, load has not completed in time,
     *                                      or an unexpected error occurs
     */
    @Override
    public NodePipeline getPipeline(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Requested {} by {} -> {}", NodePipeline.class.getSimpleName(),
                    GraphNodeRef.class.getSimpleName(), graphNodeRef);

            NodePipeline result = nodePipelineMap.get(graphNodeRef);
            if (result == null) {
                String errMessage = "Repository " + JpaLikePipelineRepository.class.getSimpleName()
                        + " does not contain " + NodePipeline.class.getSimpleName()
                        + " requested by " + graphNodeRef;
                logger.error(errMessage);
                logger.error("Repository -> {}", nodePipelineMap);
                throw new OperationIncompleteException(errMessage);
            }

            logger.debug("Returns requested {}", NodePipeline.class.getSimpleName());
            logger.trace("Returns requested {} -> {}", NodePipeline.class.getSimpleName(), result);
            return result;
        } catch (OperationIncompleteException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new OperationIncompleteException("Timed out waiting for repository load: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationIncompleteException("Interrupted while waiting for repository load", e);
        } catch (Exception e) {
            throw new OperationIncompleteException("Failed to get pipeline: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the repository content. Semantically identical to {@link #reload()}.
     *
     * @throws RuntimeException if loading fails
     */
    @Override
    public void load() {
        reload();
    }

    /**
     * Reloads repository content from the DAO in a thread-safe manner.
     * <p>
     * The method:
     * <ol>
     *   <li>Sets {@code isLoaded=false} and creates a new {@link CountDownLatch} so readers will wait for this cycle.</li>
     *   <li>Loads data from the DAO into a temporary map.</li>
     *   <li>Publishes the map (clear + putAll), sets {@code isLoaded=true}, and then calls {@code countDown()}.</li>
     * </ol>
     * If loading fails, the latch is still released and {@code isLoaded} remains {@code false}
     * to prevent indefinite blocking and to signal unsuccessful load.
     *
     * @throws RuntimeException if loading fails
     */
    @Override
    public void reload() {
        synchronized (reloadLock) {
            CountDownLatch newLatch = new CountDownLatch(1);
            loadLatch = newLatch;
            isLoaded.set(false);

            try {
                logger.debug("Reload requested for project '{}'", projectName);

                Map<GraphNodeRef, NodePipeline> loaded = pipelineDao.load(projectName);

                nodePipelineMap.clear();
                nodePipelineMap.putAll(loaded);

                isLoaded.set(true);
                newLatch.countDown();
                logger.debug("Reload finished for project '{}'", projectName);
            } catch (Exception e) {
                // Ensure readers do not block forever and state reflects an unsuccessful load
                isLoaded.set(false);
                newLatch.countDown();
                logger.error("Reload error (unexpected)", e);
                throw new RuntimeException("Reload failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Waits for the repository to finish its initial load or the latest reload.
     * <p>
     * Uses a {@link CountDownLatch} to synchronize threads and limits the waiting time.
     * The timeout prevents indefinite blocking in case of DAO hangs or load errors.
     * A fast path checks {@code isLoaded} first to avoid unnecessary waiting.
     * </p>
     *
     * @param timeout maximum time to wait
     * @param unit    time unit of the {@code timeout} argument
     * @throws InterruptedException  if the current thread is interrupted while waiting
     * @throws TimeoutException      if loading has not completed within the given time
     * @throws IllegalStateException if, after waiting, the repository is still not marked as loaded
     */
    private void awaitLoaded(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (isLoaded.get()) {
            return; // already loaded; no need to await the latch
        }
        CountDownLatch latch = this.loadLatch; // capture current cycle
        boolean ok = latch.await(timeout, unit);
        if (!ok) {
            throw new TimeoutException("Waiting for load timeout after " + timeout + " " + unit);
        }
        if (!isLoaded.get()) {
            throw new IllegalStateException("Repository is not loaded");
        }
    }
}