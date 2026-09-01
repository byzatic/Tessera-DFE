package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal;

import org.jetbrains.annotations.NotNull;

public enum NodeLifecycleState {
    NOTSTATED(0),
    WAITING(1),
    READY(2);

    private int code;

    NodeLifecycleState(@NotNull Integer code) {
        requireCode(code);
        this.code = code;
    }

    synchronized public int getCode() {
        return code;
    }

    synchronized public void setCode(@NotNull Integer code) {
        requireCode(code);
        this.code = code;
    }

    /**
     * Метод, чтобы получить enum по числу
     *
     * @param code
     * @return
     */
    synchronized public static NodeLifecycleState fromCode(@NotNull Integer code) {
        requireCode(code);
        for (NodeLifecycleState state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }

    private static void requireCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("Node lifecycle state code should be NotNull");
        }
    }

    @Override
    synchronized public String toString() {
        return "NodeLifecycleState{" +
                "code=" + code +
                '}';
    }
}
