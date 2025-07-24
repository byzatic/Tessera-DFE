package io.github.byzatic.tessera.engine.domain.business.sheduller;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.errorprone.annotations.concurrent.GuardedBy;
import org.jetbrains.annotations.NotNull;
import ru.byzatic.commons.ObjectsUtils;

@ThreadSafe
public class StatusProxy {
    private final String id;
    @GuardedBy("this") private StatusResult status = null;

    public StatusProxy(String id) {
        this.id = id;
        this.status = new StatusResult();
    }

    @NotNull
    public synchronized StatusResult getStatusResult() throws IllegalStateException {
        ObjectsUtils.requireNonNull(status, new IllegalStateException("Status was not set"));
        return status;
    }

    public synchronized void setStatus(@NotNull StatusResult status) {
        ObjectsUtils.requireNonNull(status, new IllegalArgumentException("Status must be NotNull"));
        this.status = status;
    }

    public String getId() {
        return id;
    }
}
