package io.github.byzatic.tessera.engine.domain.business.sheduller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.byzatic.commons.base_exceptions.OperationIncompleteException;
import io.github.byzatic.tessera.engine.App;
import io.github.byzatic.tessera.engine.Configuration;

public class JobWrapper implements Runnable {
    private final static Logger logger= LoggerFactory.getLogger(App.class);
    private StatusProxy statusProxy = null;
    private Runnable job = null;
    private String id = null;
    private String processDescription = null;

    public JobWrapper(StatusProxy statusProxy, Runnable job, String id, String processDescription) {
        this.statusProxy = statusProxy;
        this.job = job;
        this.id = id;
        this.processDescription = processDescription;
    }

    @Override
    public void run() {
        try (AutoCloseable ignored = Configuration.MDC_ENGINE_CONTEXT.use()) {
            logger.debug("Running JOB with id {} ({})", id, processDescription);
            statusProxy.setStatus(StatusResult.running());
            job.run();
            statusProxy.setStatus(StatusResult.complete());
            logger.debug("JOB with id {} ({}) complete", id, processDescription);
        } catch (Throwable t) {
            logger.error("JobWrapper error handled error", t);
            statusProxy.setStatus(StatusResult.fault(new OperationIncompleteException(t)));
        }
    }
}
