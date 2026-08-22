package io.github.byzatic.tessera.engine.application.commons.logging;

import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;

/**
 * Thread-confined scope that applies an MDC context and restores the previous one on close.
 */
public final class MdcContextScope implements AutoCloseable {

    private final Map<String, String> previousContext;
    private boolean closed;

    private MdcContextScope(Map<String, String> previousContext) {
        this.previousContext = previousContext;
    }

    public static MdcContextScope open(MdcContextInterface context) {
        Objects.requireNonNull(context, "context");

        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        try {
            context.apply();
            return new MdcContextScope(previousContext);
        } catch (RuntimeException exception) {
            restore(previousContext);
            throw exception;
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        restore(previousContext);
    }

    private static void restore(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            MDC.clear();
        } else {
            MDC.setContextMap(context);
        }
    }
}
