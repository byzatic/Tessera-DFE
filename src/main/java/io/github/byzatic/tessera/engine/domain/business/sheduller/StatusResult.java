package io.github.byzatic.tessera.engine.domain.business.sheduller;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatusResult {
    private final Status status;
    private final Throwable faultCause;

    public StatusResult(Status status, Throwable faultCause) {
        this.status = status;
        this.faultCause = faultCause;
    }

    public StatusResult() {
        this.status = Status.NEVER_RUN;
        this.faultCause = null;
    }

    public Status getStatus() {
        return status;
    }

    public @Nullable Throwable getFaultCause() {
        return faultCause;
    }

    public static @NotNull StatusResult running() {
        return new StatusResult(Status.RUNNING, null);
    }

    public static @NotNull StatusResult complete() {
        return new StatusResult(Status.COMPLETE, null);
    }

    public static @NotNull StatusResult fault(@NotNull Throwable cause) {
        return new StatusResult(Status.FAULT, cause);
    }

    public enum Status {
        RUNNING,
        COMPLETE,
        FAULT,
        NEVER_RUN
    }
}
