package io.github.byzatic.tessera.engine.infrastructure.observability;

import io.github.byzatic.tessera.engine.Configuration;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PrometheusMetricsAgent {

    private static final Logger logger =
            LoggerFactory.getLogger(PrometheusMetricsAgent.class);

    private static final PrometheusMetricsAgent INSTANCE =
            new PrometheusMetricsAgent();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean jvmMetricsEnabled = new AtomicBoolean(false);

    /** Unique ID per JVM run */
    private final String runId = UUID.randomUUID().toString();

    private PrometheusRegistry registry;
    private HTTPServer server;

    private Gauge graphExecutionSeconds;
    private Counter graphExecutionsTotal;
    private Gauge nodePipelineExecutionSeconds;

    private PrometheusMetricsAgent() {}

    public static PrometheusMetricsAgent getInstance() {
        return INSTANCE;
    }

    public void start(URI prometheusEndpoint) throws IOException {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        this.registry = new PrometheusRegistry();

        this.graphExecutionSeconds = Gauge.builder()
                .name("tessera_graph_execution_seconds")
                .help("Graph execution duration in seconds")
                .labelNames("run_id")
                .register(registry);

        this.graphExecutionsTotal = Counter.builder()
                .name("tessera_graph_executions_total")
                .help("Total number of completed graph executions")
                .labelNames("run_id")
                .register(registry);

        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
            this.nodePipelineExecutionSeconds = Gauge.builder()
                    .name("tessera_node_pipeline_execution_seconds")
                    .help("Node pipeline execution duration in seconds")
                    .labelNames("run_id", "node_id", "node_name", "node_path")
                    .register(registry);
        }

        this.server = HTTPServer.builder()
                .registry(registry)
                .port(prometheusEndpoint.getPort())
                .hostname(prometheusEndpoint.getHost())
                .buildAndStart();

        logger.info("Prometheus metrics started, run_id={}", runId);
    }

    public void publishGraphExecutionTime(long durationMillis) {
        ensureStarted();

        double seconds = durationMillis / 1000.0;

        graphExecutionSeconds
                .labelValues(runId)
                .set(seconds);

        if (graphExecutionsTotal != null) {
            graphExecutionsTotal
                    .labelValues(runId)
                    .inc();
        }
    }

    public void publishNodePipelineExecutionTime(
            long durationMillis,
            String nodeId,
            String nodeName,
            String nodePath
    ) {
        ensureStarted();

        if (!Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
            logger.debug("publishNodePipelineExecutionTime called but disabled");
            return;
        }

        if (nodePipelineExecutionSeconds == null) {
            logger.warn("Metric not registered");
            return;
        }

        double seconds = durationMillis / 1000.0;

        nodePipelineExecutionSeconds
                .labelValues(runId, nodeId, nodeName, nodePath)
                .set(seconds);
    }

    public void incrementGraphExecutions() {
        ensureStarted();
        if (graphExecutionsTotal != null) {
            graphExecutionsTotal
                    .labelValues(runId)
                    .inc();
        }
    }

    public void enableJvmMetrics() {
        ensureStarted();

        if (!jvmMetricsEnabled.compareAndSet(false, true)) {
            return;
        }

        JvmMetrics.builder().register(registry);
    }

    public void stop() {
        if (server != null) {
            server.close();
        }
    }

    private void ensureStarted() {
        if (!started.get()) {
            throw new IllegalStateException(
                    "PrometheusMetricsAgent is not started");
        }
    }

    public String getRunId() {
        return runId;
    }
}