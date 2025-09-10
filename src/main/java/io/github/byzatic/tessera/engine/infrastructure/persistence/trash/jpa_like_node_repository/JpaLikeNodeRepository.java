package io.github.byzatic.tessera.engine.infrastructure.persistence.trash.jpa_like_node_repository;

import io.github.byzatic.tessera.engine.infrastructure.persistence.project_loader.ProjectDaoInterface;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.GraphNodeRef;
import io.github.byzatic.tessera.engine.domain.model.node.NodeItem;
import io.github.byzatic.tessera.engine.domain.model.node.Project;
import io.github.byzatic.tessera.engine.infrastructure.persistence.trash.JpaLikeNodeRepositoryInterface;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe repository that loads and serves {@link NodeItem} instances by {@link GraphNodeRef}.
 * <p>
 * Loading/reloading is coordinated via a {@link CountDownLatch}. Readers wait (with a timeout)
 * until the current load cycle completes to avoid observing partially initialized state.
 * An {@code isLoaded} flag provides a fast path when data is already published.
 * </p>
 */
public class JpaLikeNodeRepository implements JpaLikeNodeRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(JpaLikeNodeRepository.class);

    /** Max time (in seconds) a reader waits for the repository to complete loading. */
    private final long waitTimeoutSeconds;

    private final ProjectDaoInterface projectDao;
    private final SupportProjectValidation supportProjectValidation = new SupportProjectValidation();
    private final String projectName;

    /** Storage published-before countDown() in reload(), then read after await(). */
    private final Map<GraphNodeRef, NodeItem> nodeMap = new HashMap<>();
    /** Cached list of all node refs, rebuilt on each reload. */
    private final List<GraphNodeRef> allGraphNodeRef = new ArrayList<>();

    /** Load-state and synchronization primitives for load/reload cycles. */
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);
    private volatile CountDownLatch loadLatch = new CountDownLatch(1);
    private final Object reloadLock = new Object();

    /**
     * Creates a repository with a default 10-second wait timeout for readers.
     *
     * @param projectName non-null project name
     * @param projectDao  non-null DAO used to load a {@link Project}
     * @throws OperationIncompleteException if arguments are invalid
     */
    public JpaLikeNodeRepository(@NotNull String projectName, @NotNull ProjectDaoInterface projectDao)
            throws OperationIncompleteException {
        this(projectName, projectDao, 10L);
    }

    /**
     * Creates a repository with a custom wait timeout for readers.
     *
     * @param projectName        non-null project name
     * @param projectDao         non-null DAO used to load a {@link Project}
     * @param waitTimeoutSeconds non-null timeout in seconds readers will wait for loading to finish
     * @throws OperationIncompleteException if arguments are invalid
     */
    public JpaLikeNodeRepository(@NotNull String projectName,
                                 @NotNull ProjectDaoInterface projectDao,
                                 @NotNull Long waitTimeoutSeconds) throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(projectName, new IllegalArgumentException("projectName must not be null"));
            ObjectsUtils.requireNonNull(projectDao, new IllegalArgumentException("projectDao must not be null"));
            ObjectsUtils.requireNonNull(waitTimeoutSeconds, new IllegalArgumentException("waitTimeoutSeconds must not be null"));
            this.projectName = projectName;
            this.projectDao = projectDao;
            this.waitTimeoutSeconds = waitTimeoutSeconds;
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    /**
     * Returns the node for the given reference. This method blocks (up to the configured timeout)
     * until the repository has finished its most recent load/reload cycle.
     *
     * @param graphNodeRef node reference key
     * @return non-null {@link NodeItem}
     * @throws OperationIncompleteException if the node is absent, load has not completed in time,
     *                                      or an unexpected error occurs
     */
    @Override
    public NodeItem getNode(GraphNodeRef graphNodeRef) throws OperationIncompleteException {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Requested {} by {} -> {}", NodeItem.class.getSimpleName(),
                    GraphNodeRef.class.getSimpleName(), graphNodeRef);

            NodeItem result = nodeMap.get(graphNodeRef);
            if (result == null) {
                String errMessage = "Repository " + JpaLikeNodeRepository.class.getSimpleName()
                        + " does not contain " + NodeItem.class.getSimpleName()
                        + " requested by " + graphNodeRef;
                logger.error(errMessage);
                logger.error("Repository -> {}", nodeMap);
                throw new OperationIncompleteException(errMessage);
            }

            logger.debug("Returns requested {}", NodeItem.class.getSimpleName());
            logger.trace("Returns requested {} -> {}", NodeItem.class.getSimpleName(), result);
            return result;
        } catch (OperationIncompleteException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new OperationIncompleteException("Timed out waiting for repository load: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationIncompleteException("Interrupted while waiting for repository load", e);
        } catch (Exception e) {
            throw new OperationIncompleteException("Failed to get node: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the list of all {@link GraphNodeRef}. This method blocks (up to the configured timeout)
     * until the repository has finished its most recent load/reload cycle.
     *
     * @return non-null list of {@link GraphNodeRef}
     * @throws OperationIncompleteException if load has not completed in time or an unexpected error occurs
     */
    @Override
    public List<GraphNodeRef> getAllGraphNodeRef() throws OperationIncompleteException {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Returns requested List of all {} of size {}",
                    GraphNodeRef.class.getSimpleName(), allGraphNodeRef.size());
            logger.trace("Returns requested List of all {} -> {}",
                    GraphNodeRef.class.getSimpleName(), allGraphNodeRef);
            // Return an unmodifiable snapshot to prevent external mutation.
            return Collections.unmodifiableList(new ArrayList<>(allGraphNodeRef));
        } catch (TimeoutException e) {
            throw new OperationIncompleteException("Timed out waiting for repository load: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationIncompleteException("Interrupted while waiting for repository load", e);
        } catch (Exception e) {
            throw new OperationIncompleteException("Failed to get all GraphNodeRef: " + e.getMessage(), e);
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
     *   <li>Loads the {@link Project} from disk using the configured {@code projectName}.</li>
     *   <li>Validates the project and publishes its state into {@code nodeMap} and {@code allGraphNodeRef}.</li>
     *   <li>Sets {@code isLoaded=true} and calls {@code countDown()} to release readers.</li>
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

                Path projectFilePath = Configuration.PROJECTS_DIR
                        .resolve(projectName)
                        .resolve("data")
                        .resolve("Project.json");

                Project project = projectDao.load(projectFilePath);
                logger.debug("Loaded project '{}': {}", projectName, project);

                supportProjectValidation.validate(project);

                // Publish new state (happens-before ensured by countDown()).
                nodeMap.clear();
                nodeMap.putAll(project.getNodeMap());

                allGraphNodeRef.clear();
                allGraphNodeRef.addAll(nodeMap.keySet());

                isLoaded.set(true);
                newLatch.countDown();
                logger.debug("Reload finished for project '{}'. nodeMap size={}", projectName, nodeMap.size());
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