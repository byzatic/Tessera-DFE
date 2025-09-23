package io.github.byzatic.tessera.engine.domain.business.common;

import java.nio.file.Path;

/**
 * Listener for directory changes.
 * Implementations must be fast and non-blocking; heavy work should be delegated.
 */
public interface DirectoryChangeListener {

    /**
     * Called when a file is created.
     * @param path absolute or root-relative path depending on watcher configuration
     */
    void onFileCreated(Path path);

    /**
     * Called when a file is modified.
     * @param path absolute or root-relative path depending on watcher configuration
     */
    void onFileModified(Path path);

    /**
     * Called when a file is deleted.
     * @param path absolute or root-relative path depending on watcher configuration
     */
    void onFileDeleted(Path path);

    /**
     * Called for any of the above events (created/modified/deleted), useful for auditing/metrics.
     * @param path event path
     */
    void onAny(Path path);

    /**
     * Called when watcher experiences an unexpected failure (I/O, scheduler failure, etc).
     * Implementations should be idempotent and avoid throwing.
     */
    void onFail();

    /**
     * Optional overload that forwards to {@link #onFail()} keeping binary compatibility with
     * code paths that want to pass a throwable for logging.
     * @param t optional throwable (may be null)
     */
    default void onFail(Throwable t) { onFail(); }
}
