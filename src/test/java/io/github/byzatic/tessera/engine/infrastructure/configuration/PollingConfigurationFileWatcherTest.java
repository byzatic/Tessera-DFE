package io.github.byzatic.tessera.engine.infrastructure.configuration;

import io.github.byzatic.tessera.engine.application.runtime.ConfigurationCandidateValidator;
import io.github.byzatic.tessera.engine.application.runtime.ConfigurationChangeListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PollingConfigurationFileWatcherTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void reportsStableValidChangeOnce() throws Exception {
        Path configurationFile = temporaryFolder.newFile("configuration.xml").toPath();
        Files.write(configurationFile, "initial".getBytes(StandardCharsets.UTF_8));
        RecordingListener listener = new RecordingListener();
        RecordingValidator validator = new RecordingValidator();

        PollingConfigurationFileWatcher watcher = new PollingConfigurationFileWatcher(
                configurationFile,
                Duration.ofMillis(25L),
                listener,
                validator
        );
        try {
            watcher.start();
            Files.write(configurationFile, "changed-content".getBytes(StandardCharsets.UTF_8));

            assertTrue(listener.awaitChange());
            Thread.sleep(150L);
            assertEquals(1, listener.getNotificationCount());
            assertEquals(1, validator.getValidationCount());
        } finally {
            watcher.close();
        }
    }

    @Test
    public void rejectsInvalidChangeAndAcceptsLaterReplacement() throws Exception {
        Path configurationFile = temporaryFolder.newFile("configuration.xml").toPath();
        Files.write(configurationFile, "initial".getBytes(StandardCharsets.UTF_8));
        RecordingListener listener = new RecordingListener();
        RecordingValidator validator = new RecordingValidator();
        validator.rejectCandidates();

        PollingConfigurationFileWatcher watcher = new PollingConfigurationFileWatcher(
                configurationFile,
                Duration.ofMillis(25L),
                listener,
                validator
        );
        try {
            watcher.start();
            Files.write(configurationFile, "invalid-content".getBytes(StandardCharsets.UTF_8));
            assertTrue(validator.awaitValidation());
            Thread.sleep(100L);

            assertFalse(listener.hasNotification());
            assertEquals(1, validator.getValidationCount());

            validator.acceptCandidates();
            Files.write(
                    configurationFile,
                    "valid-replacement-content".getBytes(StandardCharsets.UTF_8)
            );
            assertTrue(listener.awaitChange());
            assertEquals(1, listener.getNotificationCount());
            assertEquals(2, validator.getValidationCount());
        } finally {
            watcher.close();
        }
    }

    private static final class RecordingListener implements ConfigurationChangeListener {

        private final AtomicInteger notificationCount = new AtomicInteger();
        private final CountDownLatch notification = new CountDownLatch(1);

        @Override
        public void onConfigurationChanged() {
            notificationCount.incrementAndGet();
            notification.countDown();
        }

        private boolean awaitChange() throws InterruptedException {
            return notification.await(2L, TimeUnit.SECONDS);
        }

        private boolean hasNotification() {
            return notificationCount.get() > 0;
        }

        private int getNotificationCount() {
            return notificationCount.get();
        }
    }

    private static final class RecordingValidator
            implements ConfigurationCandidateValidator {

        private final AtomicInteger validationCount = new AtomicInteger();
        private final AtomicBoolean reject = new AtomicBoolean(false);
        private final CountDownLatch firstValidation = new CountDownLatch(1);

        @Override
        public void validate() throws Exception {
            validationCount.incrementAndGet();
            firstValidation.countDown();
            if (reject.get()) {
                throw new Exception("Rejected for test");
            }
        }

        private void rejectCandidates() {
            reject.set(true);
        }

        private void acceptCandidates() {
            reject.set(false);
        }

        private boolean awaitValidation() throws InterruptedException {
            return firstValidation.await(2L, TimeUnit.SECONDS);
        }

        private int getValidationCount() {
            return validationCount.get();
        }
    }
}
