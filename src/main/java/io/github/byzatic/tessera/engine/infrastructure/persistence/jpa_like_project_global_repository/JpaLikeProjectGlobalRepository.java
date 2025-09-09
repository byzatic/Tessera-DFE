package io.github.byzatic.tessera.engine.infrastructure.persistence.jpa_like_project_global_repository;

import io.github.byzatic.commons.ObjectsUtils;
import io.github.byzatic.tessera.engine.application.commons.exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.domain.model.project.ProjectGlobal;
import io.github.byzatic.tessera.engine.domain.model.project.StoragesItem;
import io.github.byzatic.tessera.engine.domain.repository.JpaLikeProjectGlobalRepositoryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class JpaLikeProjectGlobalRepository implements JpaLikeProjectGlobalRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(JpaLikeProjectGlobalRepository.class);

    private final long waitTimeoutSeconds;

    private final ProjectGlobalDaoInterface projectGlobalDao;
    private final String projectName;

    private volatile ProjectGlobal projectGlobal = null;
    private final AtomicBoolean isLoaded = new AtomicBoolean(false);

    private final Object reloadLock = new Object();
    private volatile CountDownLatch loadLatch = new CountDownLatch(1);

    public JpaLikeProjectGlobalRepository(String projectName, ProjectGlobalDaoInterface projectGlobalDao)
            throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(projectName, new IllegalArgumentException("ProjectName should not be null"));
            ObjectsUtils.requireNonNull(projectGlobalDao, new IllegalArgumentException("ProjectGlobalDao should not be null"));
            this.projectName = projectName;
            this.projectGlobal = null;
            this.projectGlobalDao = projectGlobalDao;
            this.waitTimeoutSeconds = 10;
            // loadLatch уже = 1 => методы будут ждать первой загрузки
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    public JpaLikeProjectGlobalRepository(String projectName, ProjectGlobalDaoInterface projectGlobalDao, Long waitTimeoutSeconds)
            throws OperationIncompleteException {
        try {
            ObjectsUtils.requireNonNull(projectName, new IllegalArgumentException("ProjectName should not be null"));
            ObjectsUtils.requireNonNull(projectGlobalDao, new IllegalArgumentException("ProjectGlobalDao should not be null"));
            this.projectName = projectName;
            this.projectGlobal = null;
            this.projectGlobalDao = projectGlobalDao;
            this.waitTimeoutSeconds = waitTimeoutSeconds;
            // loadLatch уже = 1 => методы будут ждать первой загрузки
        } catch (Exception e) {
            throw new OperationIncompleteException(e.getMessage(), e);
        }
    }

    @Override
    public ProjectGlobal getProjectGlobal() {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);
            ProjectGlobal pg = this.projectGlobal;
            logger.debug("Returns requested {} -> {}", ProjectGlobal.class.getSimpleName(), pg);
            return pg;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ProjectGlobal: " + e.getMessage(), e);
        }
    }

    @Override
    public Boolean containsProjectGlobalForStorage(String storageId) {
        try {
            awaitLoaded(waitTimeoutSeconds, TimeUnit.SECONDS);
            final ProjectGlobal pg = this.projectGlobal;
            if (pg == null) return Boolean.FALSE;

            final List<StoragesItem> storages = pg.getStorages();
            if (storages == null || storages.isEmpty()) return Boolean.FALSE;

            for (StoragesItem storageItem : storages) {
                if (Objects.equals(storageItem.getIdName(), storageId)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        } catch (Exception e) {
            throw new RuntimeException("Failed in containsProjectGlobalForStorage: " + e.getMessage(), e);
        }
    }

    @Override
    public void load() {
        try {
            reload();
        } catch (Exception e) {
            logger.error("Load error", e);
            throw e;
        }
    }

    @Override
    public void reload() {
        synchronized (reloadLock) {
            try {
                logger.debug("Reload requested for project '{}'", projectName);
                isLoaded.set(false);

                CountDownLatch newLatch = new CountDownLatch(1);
                loadLatch = newLatch;

                projectGlobal = projectGlobalDao.load(projectName);
                isLoaded.set(true);

                newLatch.countDown();
                logger.debug("Reload finished for project '{}'", projectName);
            } catch (Exception e) {
                logger.error("Reload error", e);
                // в случае ошибки тоже отпускаем ожидающих, чтобы они получили исключение по isLoaded
                isLoaded.set(false);
                loadLatch.countDown();
                throw new RuntimeException("Reload failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Waits for the completion of loading or reloading of the {@link ProjectGlobal} object.
     * <p>
     * Uses a {@link CountDownLatch} to synchronize threads and limits the waiting
     * time to 10 seconds. The timeout is necessary because an unlimited wait
     * could lead to an indefinite block and effectively freeze the program
     * in case of loading errors or DAO hangs.
     * </p>
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the {@code timeout} argument
     * @throws InterruptedException  if the current thread is interrupted while waiting
     * @throws TimeoutException      if the loading has not completed within the given time
     * @throws IllegalStateException if, after waiting, the object was still not successfully loaded
     */
    private void awaitLoaded(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        if (isLoaded.get() && projectGlobal != null) {
            return;
        }
        CountDownLatch latch = this.loadLatch;
        boolean ok = latch.await(timeout, unit);
        if (!ok) {
            throw new TimeoutException("Waiting for load timeout after " + timeout + " " + unit);
        }
        if (!isLoaded.get() || projectGlobal == null) {
            throw new IllegalStateException("Repository is not loaded");
        }
    }
}