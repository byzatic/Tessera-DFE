package io.github.byzatic.tessera.engine.infrastructure.service.graph_reactor.graph_manager.graph_traversal.sheduller.health;

public enum HealthFlag {
    RUNNING(0),
    FINISHED(1),
    ERROR(2);

    private final int code;

    HealthFlag(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * Метод, чтобы получить enum по числу
     * @param code
     * @return
     */
    public static HealthFlag fromCode(int code) {
        for (HealthFlag state : values()) {
            if (state.code == code) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }

    @Override
    public String toString() {
        return "HealthFlagState{" +
                "code=" + code +
                '}';
    }
}
