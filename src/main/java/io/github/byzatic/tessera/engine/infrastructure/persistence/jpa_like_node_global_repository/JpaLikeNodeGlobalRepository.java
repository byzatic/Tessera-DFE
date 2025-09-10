package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_node_global_repository;

import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node_global.NodeGlobal;
import io.github.byzatic.tessera.engine.domain.model.node_global.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeNodeGlobalRepositoryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe repository that loads and serves {@link NodeGlobal} instances by {@link GraphNodeRef}.
 * <p>
 * Loading/reloading is coordinated via a {@link CountDownLatch}. Readers wait (with a timeout)
 * until the current load cycle completes to avoid observing partially initialized state.
 * An {@code isLoaded} flag provides a fast path when data is already published.
 * </p>
 */
public class JpaLikeNodeGlobalRepository implements JpaLikeNodeGlobalRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(JpaLikeNodeGlobalRepository.class);

    /** Max time (in seconds) a reader waits for the repository to complete loading. */
    private final long waitTimeoutSeconds;

    private final NodeGlobalDaoInterface nodeGlobalDao;
    private final String projectName;

    /** Storage published-before countDown() in reload(), then read after await(). */
    private final Map<GraphNodeRef, NodeGlobal> nodeGlobalMap = new HashMap<>();

    /** Load-state and synchronization primitives for load/reload cycles. */
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private volatile CountDownLatch loadLatch = new CountDownLatch(1);
    private final Object reloadLock = new Object();

    /**
     * Creates a repository with a default 10-second wait timeout for readers.
     *
     * @param projectName  non-null project name
     * @param nodeGlobalDao non-null DAO used to load node-global data
     * @throws OperationIncompleteException if arguments are invalid
     */
    public JpaLikeNodeGlobalRepository(String projectName, NodeGlobalDaoInterface nodeGlobalDao)
            throws OperationIncompleteException {
        this(projectName, nodeGlobalDao, 10L);
    }

    /**
     * Creates a repository with a custom wait timeout for readers.
     *
     * @param projectName        non-null project name
     * @param nodeGlobalDao      non-null DAO used to load node-global data
     * @param waitTimeoutSeconds non-null timeout in seconds readers will wait for loading to finish
     * @throws OperationIncompleteException if arguments are invalid
     */
    public JpaLikeNodeGlobalRepository(String projectName,
                                       NodeGlobalDaoInterface nodeGlobalDao,
                                       Long waitTimeoutSeconds) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(projectName, new IllegalArgumentException("ProjectName must not be null"));
            ObjectsUtils.requireNonNull(nodeGlobalDao, new IllegalArgumentException("NodeGlobalDao must not be null"));
            ObjectsUtils.requireNonNull(waitTimeoutSeconds, new IllegalArgumentException("waitTimeoutSeconds must not be null"));
            this.projectName = projectName;
            this.nodeGlobalDao = nodeGlobalDao;
            this.waitTimeoutSeconds = waitTimeoutSeconds;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    /**
     * Returns the {@link NodeGlobal} for the given reference. This method blocks (up to the configured timeout)
     * until the repository has finished its most recent load/reload cycle.
     *
     * @param graphNodeRef node reference key
     * @return non-null {@link NodeGlobal}
     * @throws OperationIncompleteException if the node is absent, load has not completed in time,
     *                                      or an unexpected error occurs
     */
    @Override
    public NodeGlobal getNodeGlobal(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Requested {} by {} -> {}", NodeGlobal.class.getSimpleName(),
                    GraphNodeRef.class.getSimpleName(), graphNodeRef);

            NodeGlobal result = nodeGlobalMap.get(graphNodeRef);
            if (result == null) {
                String errMessage = "Repository " + JpaLikeNodeGlobalRepository.class.getSimpleName()
                        + " does not contain " + NodeGlobal.class.getSimpleName()
                        + " requested by " + graphNodeRef;
                logger.error(errMessage);
                logger.error("Repository -> {}", nodeGlobalMap);
                throw new OperationIncompleteException(errMessage);
            }

            logger.debug("Returns requested {}", NodeGlobal.class.getSimpleName());
            logger.trace("Returns requested {} -> {}", NodeGlobal.class.getSimpleName(), result);
            return result;
        } catch (OperationIncompleteException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new OperationIncompleteException("Timed out waiting for repository load: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationIncompleteException("Interrupted while waiting for repository load", e);
        } catch (Exception e) {
            throw new OperationIncompleteException("Failed to get node-global: " + e.getMessage(), e);
        }
    }

    /**
     * Checks whether the {@link NodeGlobal} for the given reference contains a storage entry
     * with the specified ID (name). This method blocks (up to the configured timeout)
     * until the repository has finished its most recent load/reload cycle.
     *
     * @param graphNodeRef node reference key
     * @param storageName  storage id/name to check
     * @return {@code true} if a storage with the given id is present, otherwise {@code false}
     * @throws RuntimeException if load has not completed in time or an unexpected error occurs
     */
    @Override
    public Boolean isStorageWithId(GraphNodeRef graphNodeRef, String storageName) {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);

            NodeGlobal ng = nodeGlobalMap.get(graphNodeRef);
            if (ng == null) {
                return Boolean.FALSE;
            }

            List<StoragesItem> storages = ng.getStorages();
            if (storages == null || storages.isEmpty()) {
                return Boolean.FALSE;
            }

            for (StoragesItem storageItem : storages) {
                if (Objects.equals(storageItem.getIdName(), storageName)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        } catch (TimeoutException e) {
            throw new RuntimeException("Timed out waiting for repository load: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for repository load", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check storage id: " + e.getMessage(), e);
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

                Map<GraphNodeRef, NodeGlobal> loaded = nodeGlobalDao.load(projectName);

                nodeGlobalMap.clear();
                nodeGlobalMap.putAll(loaded);

                isLoaded.set(true);
                newLatch.countDown();
                logger.debug("Reload finished for project '{}'. nodeGlobalMap size={}", projectName, nodeGlobalMap.size());
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