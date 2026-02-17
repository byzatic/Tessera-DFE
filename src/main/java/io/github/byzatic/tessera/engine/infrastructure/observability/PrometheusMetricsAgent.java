package io.github.byzatic.tessera.engine.infrastructure.observability;

import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PrometheusMetricsAgent {

    private static final PrometheusMetricsAgent INSTANCE = new PrometheusMetricsAgent();

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean jvmMetricsEnabled = new AtomicBoolean(false);

    private PrometheusRegistry registry;
    private HTTPServer server;

    private Gauge graphExecutionSeconds;

    private PrometheusMetricsAgent() {
        // singleton
    }

    public static PrometheusMetricsAgent getInstance() {
        return INSTANCE;
    }

    /**
     * Starts Prometheus HTTP endpoint (e.g. http://0.0.0.0:9095/metrics) and registers metrics.
     * Safe to call multiple times; only first call actually starts.
     */
    public void start(URI prometheusEndpoint) throws IOException {
        if (!started.compareAndSet(false, true)) {
            return; // already started
        }

        this.registry = new PrometheusRegistry();

        this.graphExecutionSeconds = Gauge.builder()
                .name("tessera_graph_execution_seconds")
                .help("Graph execution duration in seconds")
                .register(registry);

        // Start HTTP endpoint
        this.server = HTTPServer.builder()
                .registry(registry)
                .port(prometheusEndpoint.getPort())
                .hostname(prometheusEndpoint.getHost())
                .buildAndStart();
    }

    /**
     * Publish graph execution duration.
     */
    public void publishGraphExecutionTime(long durationMillis) {
        ensureStarted();

        double seconds = durationMillis / 1000.0;

        graphExecutionSeconds.set(seconds);
    }

    /**
     * Optional: stop endpoint on shutdown.
     */
    public void stop() {
        if (server != null) {
            server.close();
        }
    }

    private void ensureStarted() {
        if (!started.get()) {
            throw new IllegalStateException("PrometheusMetricsAgent is not started. Call start(host, port) first.");
        }
    }

    public void enableJvmMetrics() {
        ensureStarted();

        if (!jvmMetricsEnabled.compareAndSet(false, true)) {
            return;
        }

        JvmMetrics.builder()
                .register(registry);
    }

}
