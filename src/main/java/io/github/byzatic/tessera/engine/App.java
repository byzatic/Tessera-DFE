package io.github.byzatic.tessera.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.byzatic.tessera.engine.application.usecase.ProjectOrchestrator;
import io.github.byzatic.tessera.engine.infrastructure.config.ApplicationContext;

public class App {
    private final static Logger logger= LoggerFactory.getLogger(App.class);
    private final static ProjectOrchestrator projectOrchestrator = new ProjectOrchestrator(ApplicationContext.getDomainLogic());

    public static void main( String[] args ) {
        try {
            logger.debug("Run application {} version {}", Configuration.APP_NAME, Configuration.APP_VERSION);

            projectOrchestrator.orchestrateProject();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
