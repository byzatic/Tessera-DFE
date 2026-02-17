package io.github.byzatic.tessera.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.usecase.ProjectOrchestrator;
import io.github.byzatic.tessera.engine.infrastructure.config.ApplicationMainContext;

public class App {
    private final static Logger logger= LoggerFactory.getLogger(App.class);

    public static void main( String[] args ) {
        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.debug("Run application {} version {}", Configuration.APP_NAME, Configuration.APP_VERSION);

            new ProjectOrchestrator(ApplicationMainContext.getDomainLogic()).orchestrateProject();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
