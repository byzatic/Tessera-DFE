package io.github.byzatic.tessera.engine.infrastructure.runtime;

import io.github.byzatic.tessera.engine.Configuration;
import io.github.byzatic.tessera.engine.application.runtime.ConfigurationChangeListener;
import io.github.byzatic.tessera.engine.infrastructure.configuration.PollingConfigurationFileWatcher;
import io.github.byzatic.tessera.engine.infrastructure.configuration.XmlConfigurationCandidateValidator;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supervises complete engine runtimes and replaces them after configuration changes.
 *
 * <p>The configuration watcher only requests a reload. Runtime shutdown, configuration
 * publication, and construction of the replacement runtime are serialized by the thread calling
 * {@link #run()}. No old and new project runtime can execute concurrently.</p>
 */
public final class EngineSupervisor
        implements ConfigurationChangeListener, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(EngineSupervisor.class);
    private static final long DEFAULT_WATCH_INTERVAL_MILLIS = 1000L;

    private final PollingConfigurationFileWatcher configurationWatcher;
    private final AtomicReference<TesseraEngineLifecycleManager> activeLifecycle =
            new AtomicReference<TesseraEngineLifecycleManager>();
    private final AtomicBoolean reloadRequested = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private EngineSupervisor(Duration watchInterval) {
        this.configurationWatcher = createConfigurationWatcher(watchInterval);
    }

    /**
     * Creates the default engine supervisor from bootstrap system properties.
     *
     * @return configured engine supervisor
     */
    public static EngineSupervisor createDefault() {
        return new EngineSupervisor(readConfigurationWatchInterval());
    }

    /**
     * Runs engine runtimes until application shutdown or an unrecoverable runtime failure.
     *
     * @throws Exception when watcher startup or a managed engine runtime fails
     */
    public void run() throws Exception {
        configurationWatcher.start();
        while (!closed.get()) {
            TesseraEngineLifecycleManager lifecycle = createLifecycleManager();
            activeLifecycle.set(lifecycle);
            if (reloadRequested.get() || closed.get()) {
                lifecycle.close();
            }
            try {
                lifecycle.run();
            } finally {
                activeLifecycle.compareAndSet(lifecycle, null);
                lifecycle.close();
            }

            if (closed.get() || !reloadRequested.getAndSet(false)) {
                return;
            }
            reloadConfiguration();
        }
    }

    @Override
    public void onConfigurationChanged() {
        if (closed.get()) {
            return;
        }
        reloadRequested.set(true);
        TesseraEngineLifecycleManager lifecycle = activeLifecycle.get();
        if (lifecycle != null) {
            logger.info("Stopping active engine runtime to apply configuration change");
            lifecycle.close();
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        configurationWatcher.close();
        TesseraEngineLifecycleManager lifecycle = activeLifecycle.get();
        if (lifecycle != null) {
            lifecycle.close();
        }
    }

    /**
     * Creates a complete lifecycle for the currently published configuration.
     *
     * @return new engine lifecycle
     */
    private TesseraEngineLifecycleManager createLifecycleManager() {
        return TesseraEngineLifecycleManager.createDefault();
    }

    /**
     * Creates the process-scoped configuration file watcher.
     *
     * @param watchInterval polling interval
     * @return configuration watcher
     */
    private PollingConfigurationFileWatcher createConfigurationWatcher(
            Duration watchInterval
    ) {
        return new PollingConfigurationFileWatcher(
                Configuration.CONFIGURATION_FILE_PATH,
                watchInterval,
                this,
                new XmlConfigurationCandidateValidator()
        );
    }

    /**
     * Publishes a previously validated configuration candidate.
     *
     * <p>If the file changed again between validation and publication, the current settings are
     * retained and the previous runtime is reconstructed.</p>
     */
    private void reloadConfiguration() {
        try {
            Configuration.reload();
            logger.info("Engine configuration change applied");
        } catch (ConfigurationException exception) {
            logger.error(
                    "Cannot apply changed configuration; restarting with previous settings",
                    exception
            );
        }
    }

    /**
     * Reads the immutable bootstrap polling interval.
     *
     * @return positive polling interval
     */
    private static Duration readConfigurationWatchInterval() {
        String configuredValue = System.getProperty("configurationWatchIntervalMillis");
        if (configuredValue == null) {
            return Duration.ofMillis(DEFAULT_WATCH_INTERVAL_MILLIS);
        }
        try {
            long milliseconds = Long.parseLong(configuredValue);
            if (milliseconds <= 0L) {
                throw new IllegalArgumentException(
                        "configurationWatchIntervalMillis must be greater than zero"
                );
            }
            return Duration.ofMillis(milliseconds);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "configurationWatchIntervalMillis must be a whole number",
                    exception
            );
        }
    }
}
