package io.github.byzatic.tessera.engine.application.commons.logging;

import org.slf4j.MDC;
import io.github.byzatic.tessera.enginecommon.logging.MdcContextInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Класс для установки и снятия MDC-контекста с данными о выполняемой ноде, стейдже и рутине.
 */
public class MdcServiceContext implements MdcContextInterface {

    private Map<String, String> contextMap = new HashMap<>();


    public MdcServiceContext() {
    }

    public MdcServiceContext(MdcServiceContext.Builder builder) {
//        this.contextMap = builder.contextMap;
        this.contextMap = new HashMap<>(builder.contextMap);
    }

    public static MdcServiceContext.Builder newBuilder() {
        return new MdcServiceContext.Builder();
    }

    public static MdcServiceContext.Builder newBuilder(MdcServiceContext copy) {
        MdcServiceContext.Builder builder = new MdcServiceContext.Builder();
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
        MdcServiceContext that = (MdcServiceContext) o;
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
        private String serviceName;
        private String msgName = "identificationMessage";

        private Builder() {
        }

        public Builder setServiceName(String serviceName) {
            contextMap.put("serviceName", serviceName);
            this.serviceName = serviceName;
            return this;
        }

        public Builder clear() {
            contextMap.clear();
            serviceName = null;
            return this;
        }

        /**
         * Returns a {@code MdcContext} built from the parameters previously set.
         *
         * @return a {@code MdcContext} built with parameters of this {@code MdcContext.Builder}
         */
        public MdcServiceContext build() {
            String msgString = "type=Service"+" "+this.serviceName;
            contextMap.put(msgName, msgString);
            return new MdcServiceContext(this);
        }

    }

}
