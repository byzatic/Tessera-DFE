package io.github.byzatic.tessera.engine.application.commons.logging;

import org.slf4j.MDC;
import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Класс для установки и снятия MDC-контекста с данными о выполняемой ноде, стейдже и рутине.
 */
public class MdcWorkflowRoutineContext implements MdcContextInterface {

    private Map<String, String> contextMap = new HashMap<>();


    public MdcWorkflowRoutineContext() {
    }

    public MdcWorkflowRoutineContext(MdcWorkflowRoutineContext.Builder builder) {
//        this.contextMap = builder.contextMap;
        this.contextMap = new HashMap<>(builder.contextMap);
    }

    public static MdcWorkflowRoutineContext.Builder newBuilder() {
        return new MdcWorkflowRoutineContext.Builder();
    }

    public static MdcWorkflowRoutineContext.Builder newBuilder(MdcWorkflowRoutineContext copy) {
        MdcWorkflowRoutineContext.Builder builder = new MdcWorkflowRoutineContext.Builder();
        builder.contextMap = copy.getContextMap();
        return builder;
    }

    public Map<String, String> getContextMap() {
        return contextMap;
    }

    /**
     * Устанавливает все значения в MDC.
     */
    @Override
    public void apply() {
        for (Map.Entry<String, String> entry : contextMap.entrySet()) {
            MDC.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Удаляет все ранее установленные значения из MDC.
     */
    @Override
    public void clear() {
        for (String key : contextMap.keySet()) {
            MDC.remove(key);
        }
    }

    /**
     * Утилита для использования с try-with-resources.
     */
    @Override
    public AutoCloseable use() {
        apply();
        return this::clear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MdcWorkflowRoutineContext that = (MdcWorkflowRoutineContext) o;
        return Objects.equals(contextMap, that.contextMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextMap);
    }

    @Override
    public String toString() {
        return "MdcContext{" +
                "contextMap=" + contextMap +
                '}';
    }

    /**
     * {@code MdcContext} builder static inner class.
     */
    public static final class Builder {
        private Map<String, String> contextMap = new HashMap<>();
        private String nodeId;
        private String node;
        private String stageId;
        private String stage;
        private String routineId;
        private String routine;
        private String msgName = "identificationMessage";

        private Builder() {
        }

        public Builder setNodeIndex(String nodeIndex) {
            contextMap.put("nodeId", nodeIndex);
            this.nodeId = nodeIndex;
            return this;
        }

        public Builder setNodeName(String nodeName) {
            contextMap.put("jpa_like_node_repository", nodeName);
            this.node = nodeName;
            return this;
        }

        public Builder setStageIndex(String stageIndex) {
            contextMap.put("stageId", String.valueOf(stageIndex));
            this.stageId = stageIndex;
            return this;
        }

        public Builder setStageName(String stageName) {
            contextMap.put("stage", String.valueOf(stageName));
            this.stage = stageName;
            return this;
        }

        public Builder setRoutineIndex(String routineIndex) {
            contextMap.put("routineId", routineIndex);
            this.routineId = routineIndex;
            return this;
        }

        public Builder setRoutineName(String routineName) {
            contextMap.put("routine", routineName);
            this.routine = routineName;
            return this;
        }


        public Builder clear() {
            contextMap.clear();
            node = null;
            stageId = null;
            stage = null;
            routineId = null;
            routine = null;
            return this;
        }

        /**
         * Returns a {@code MdcContext} built from the parameters previously set.
         *
         * @return a {@code MdcContext} built with parameters of this {@code MdcContext.Builder}
         */
        public MdcWorkflowRoutineContext build() {
            String msgString = "type=WorkflowRoutine"+" "+this.node+"::"+this.stageId+":"+this.stage+"|"+this.routine+":"+this.routineId;
            contextMap.put(msgName, msgString);
            return new MdcWorkflowRoutineContext(this);
        }

    }

}
