package io.github.byzatic.tessera.engine.infrastructure.configuration;

import io.github.byzatic.tessera.engine.application.runtime.ConfigurationCandidateValidator;
import io.github.byzatic.tessera.engine.application.runtime.ConfigurationChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the engine configuration file and reports stable, valid replacements.
 *
 * <p>The watcher is thread-safe. Observation state is confined to its single executor thread.
 * A change must have the same file signature in two consecutive observations before it is
 * validated. This prevents partially written files from restarting the engine.</p>
 */
public final class PollingConfigurationFileWatcher implements AutoCloseable {

    private static final Logger logger =
            LoggerFactory.getLogger(PollingConfigurationFileWatcher.class);
    private static final int REQUIRED_STABLE_OBSERVATIONS = 2;

    private final Path configurationFile;
    private final Duration pollInterval;
    private final ConfigurationChangeListener listener;
    private final ConfigurationCandidateValidator validator;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private FileSignature acceptedSignature;
    private FileSignature rejectedSignature;
    private FileSignature candidateSignature;
    private int stableObservations;

    /**
     * Creates a polling watcher for one engine configuration file.
     *
     * @param configurationFile configuration file to observe
     * @param pollInterval positive interval between observations
     * @param listener listener notified after a stable candidate passes validation
     * @param validator candidate configuration validator
     */
    public PollingConfigurationFileWatcher(
            Path configurationFile,
            Duration pollInterval,
            ConfigurationChangeListener listener,
            ConfigurationCandidateValidator validator
    ) {
        this.configurationFile = Objects.requireNonNull(
                configurationFile,
                "configurationFile"
        ).toAbsolutePath().normalize();
        this.pollInterval = requirePositive(pollInterval);
        this.listener = Objects.requireNonNull(listener, "listener");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.executor = Executors.newSingleThreadScheduledExecutor(new WatcherThreadFactory());
    }

    /**
     * Starts configuration observation.
     *
     * @throws IOException when the initial file state cannot be read
     */
    public void start() throws IOException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Configuration watcher is already started");
        }
        if (closed.get()) {
            throw new IllegalStateException("Configuration watcher is already closed");
        }
        acceptedSignature = readSignature();
        executor.scheduleWithFixedDelay(
                new PollCommand(),
                pollInterval.toMillis(),
                pollInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        executor.shutdownNow();
    }

    private void poll() {
        if (closed.get()) {
            return;
        }
        try {
            FileSignature observed = readSignature();
            if (observed.equals(acceptedSignature)) {
                resetCandidate();
                return;
            }
            if (observed.equals(rejectedSignature)) {
                resetCandidate();
                return;
            }
            observeCandidate(observed);
            if (stableObservations < REQUIRED_STABLE_OBSERVATIONS) {
                return;
            }
            validateAndNotify(observed);
        } catch (IOException exception) {
            logger.error("Cannot observe configuration file {}", configurationFile, exception);
        } catch (RuntimeException exception) {
            logger.error("Unexpected configuration watcher failure", exception);
        }
    }

    private void validateAndNotify(FileSignature observed) throws IOException {
        try {
            validator.validate();
        } catch (Exception exception) {
            rejectedSignature = observed;
            resetCandidate();
            logger.error(
                    "Changed configuration file {} is invalid; active configuration is preserved",
                    configurationFile,
                    exception
            );
            return;
        }
        FileSignature confirmed = readSignature();
        if (!observed.equals(confirmed)) {
            observeCandidate(confirmed);
            return;
        }
        acceptedSignature = observed;
        rejectedSignature = null;
        resetCandidate();
        logger.info("Stable configuration change detected in {}", configurationFile);
        listener.onConfigurationChanged();
    }

    private void observeCandidate(FileSignature observed) {
        if (observed.equals(candidateSignature)) {
            stableObservations++;
            return;
        }
        candidateSignature = observed;
        stableObservations = 1;
    }

    private void resetCandidate() {
        candidateSignature = null;
        stableObservations = 0;
    }

    private FileSignature readSignature() throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(
                configurationFile,
                BasicFileAttributes.class
        );
        return new FileSignature(
                attributes.size(),
                attributes.lastModifiedTime(),
                attributes.fileKey()
        );
    }

    private static Duration requirePositive(Duration duration) {
        Objects.requireNonNull(duration, "pollInterval");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("pollInterval must be greater than zero");
        }
        return duration;
    }

    private final class PollCommand implements Runnable {

        @Override
        public void run() {
            poll();
        }
    }

    private static final class FileSignature {

        private final long size;
        private final FileTime lastModifiedTime;
        private final Object fileKey;

        private FileSignature(long size, FileTime lastModifiedTime, Object fileKey) {
            this.size = size;
            this.lastModifiedTime = lastModifiedTime;
            this.fileKey = fileKey;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof FileSignature)) {
                return false;
            }
            FileSignature other = (FileSignature) object;
            return size == other.size
                    && lastModifiedTime.equals(other.lastModifiedTime)
                    && Objects.equals(fileKey, other.fileKey);
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(size);
            result = 31 * result + lastModifiedTime.hashCode();
            result = 31 * result + Objects.hashCode(fileKey);
            return result;
        }
    }

    private static final class WatcherThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "configuration-file-watcher");
            thread.setDaemon(false);
            return thread;
        }
    }
}
