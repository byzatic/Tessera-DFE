package io.github.byzatic.tessera.engine;

import io.github.byzatic.tessera.engine.infrastructure.runtime.TesseraEngineLifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVM entry point for Tessera DFE.
 */
public final class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private App() {
    }

    public static void main(String[] args) {
        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use();
             TesseraEngineLifecycleManager lifecycleManager =
                     TesseraEngineLifecycleManager.createDefault()) {
            logger.debug(
                    "Run application {} version {}",
                    Configuration.APP_NAME,
                    Configuration.APP_VERSION
            );
            lifecycleManager.run();
        } catch (Exception exception) {
            throw new RuntimeException("Tessera DFE terminated with an error", exception);
        }
    }
}
