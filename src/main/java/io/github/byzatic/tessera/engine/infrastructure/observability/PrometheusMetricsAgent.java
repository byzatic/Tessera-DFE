package io.github.byzatic.tessera.engine.infrastructure.observability;

import io.github.byzatic.tessera.engine.Configuration;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PrometheusMetricsAgent {
    private final static Logger logger = LoggerFactory.getLogger(PrometheusMetricsAgent.class);

    private static final PrometheusMetricsAgent INSTANCE = new PrometheusMetricsAgent();

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final AtomicBoolean jvmMetricsEnabled = new AtomicBoolean(false);

    private PrometheusRegistry registry;
    private HTTPServer server;

    private Gauge graphExecutionSeconds = null;
    private Gauge nodePipelineExecutionSeconds = null;

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

        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
            this.nodePipelineExecutionSeconds = Gauge.builder()
                    .name("tessera_node_pipeline_execution_seconds")
                    .help("Node pipeline execution duration in seconds")
                    .labelNames("node_id", "node_name", "node_path")
                    .register(registry);
        }

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
     * Publish graph execution duration.
     */
    public void publishNodePipelineExecutionTime(long durationMillis, String nodeId, String nodeName, String nodePath) {
        ensureStarted();
        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {

            double seconds = durationMillis / 1000.0;

            nodePipelineExecutionSeconds.labelValues(nodeId, nodeName, nodePath).set(seconds);
        } else {
            logger.debug("publishNodePipelineExecutionTime(...) called but PUBLISH_NODE_PIPELINE_EXECUTION_TIME is False");
        }
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
