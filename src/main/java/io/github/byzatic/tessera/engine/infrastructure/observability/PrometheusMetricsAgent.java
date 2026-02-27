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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prometheus metrics agent for Tessera DFE.
 *
 * Notes:
 * - No per-run IDs in labels (prevents high-cardinality series explosion).
 * - Graph execution duration is a Gauge representing the LAST completed run duration.
 * - Graph executions total is a Counter incremented ONCE per completed run.
 * - Storage metrics are published as aggregated snapshots (global vs node scope),
 *   plus per storage_id aggregates (without per-node labels).
 */
public final class PrometheusMetricsAgent {

    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsAgent.class);
    private static final PrometheusMetricsAgent INSTANCE = new PrometheusMetricsAgent();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean jvmMetricsEnabled = new AtomicBoolean(false);

    private PrometheusRegistry registry;
    private HTTPServer server;

    // Graph metrics
    private Gauge graphExecutionSeconds;     // last-run duration
    private Counter graphExecutionsTotal;    // total completed runs

    // Node pipeline metrics (last-run per node pipeline)
    private Gauge nodePipelineExecutionSeconds; // {node_id,node_name,node_path}

    // Storage metrics (snapshots)
    private Gauge storagesCount;             // {scope}
    private Gauge storageItemsCount;         // {scope}
    private Gauge storageItems;              // {scope,storage_id}

    private PrometheusMetricsAgent() {
        // singleton
    }

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
                .help("Graph execution duration in seconds (duration of the last completed run)")
                .register(registry);

        this.graphExecutionsTotal = Counter.builder()
                .name("tessera_graph_executions_total")
                .help("Total number of completed graph executions")
                .register(registry);

        if (Configuration.PUBLISH_NODE_PIPELINE_EXECUTION_TIME) {
            this.nodePipelineExecutionSeconds = Gauge.builder()
                    .name("tessera_node_pipeline_execution_seconds")
                    .help("Node pipeline execution duration in seconds (duration of the last completed pipeline run for the node)")
                    // WARNING: node_path can be high-cardinality for huge graphs.
                    .labelNames("node_id", "node_name", "node_path")
                    .register(registry);
        }

        // Storage snapshots (bounded number of series)
        this.storagesCount = Gauge.builder()
                .name("tessera_storages_count")
                .help("Number of storages currently allocated")
                .labelNames("scope") // global|node
                .register(registry);

        this.storageItemsCount = Gauge.builder()
                .name("tessera_storage_items_count")
                .help("Total number of items across storages")
                .labelNames("scope") // global|node
                .register(registry);

        this.storageItems = Gauge.builder()
                .name("tessera_storage_items")
                .help("Number of items per storage_id (aggregated, no per-node labels)")
                .labelNames("scope", "storage_id") // global|node, storage_id
                .register(registry);

        this.server = HTTPServer.builder()
                .registry(registry)
                .port(prometheusEndpoint.getPort())
                .hostname(prometheusEndpoint.getHost())
                .buildAndStart();

        logger.info("Prometheus metrics started on {}:{}", prometheusEndpoint.getHost(), prometheusEndpoint.getPort());
    }

    /**
     * Publish graph execution duration (seconds) and increment total executions.
     * Call this ONCE per completed graph run.
     */
    public void publishGraphExecutionTime(long durationMillis) {
        ensureStarted();

        double seconds = durationMillis / 1000.0;

        graphExecutionSeconds.set(seconds);
        graphExecutionsTotal.inc();
    }

    /**
     * Publish node pipeline execution duration (seconds).
     * Call this when a node pipeline run completes.
     */
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
            logger.warn("tessera_node_pipeline_execution_seconds is not registered");
            return;
        }

        double seconds = durationMillis / 1000.0;

        // Defensive null-to-empty to avoid NPE from labelValues()
        String safeNodeId = (nodeId != null) ? nodeId : "";
        String safeNodeName = (nodeName != null) ? nodeName : "";
        String safeNodePath = (nodePath != null) ? nodePath : "";

        nodePipelineExecutionSeconds
                .labelValues(safeNodeId, safeNodeName, safeNodePath)
                .set(seconds);
    }

    /**
     * Publish storage snapshot metrics.
     *
     * The "node" scope is an aggregate across ALL nodes (no node_id label),
     * so Prometheus series count stays bounded.
     */
    public void publishStorageSnapshot(
            long globalStorages,
            long nodeStorages,
            long globalItems,
            long nodeItems,
            Map<String, Long> globalItemsByStorageId,
            Map<String, Long> nodeItemsByStorageId
    ) {
        ensureStarted();

        storagesCount.labelValues("global").set(globalStorages);
        storagesCount.labelValues("node").set(nodeStorages);

        storageItemsCount.labelValues("global").set(globalItems);
        storageItemsCount.labelValues("node").set(nodeItems);

        if (globalItemsByStorageId != null) {
            for (Map.Entry<String, Long> e : globalItemsByStorageId.entrySet()) {
                String storageId = (e.getKey() != null) ? e.getKey() : "";
                long v = (e.getValue() != null) ? e.getValue() : 0L;
                storageItems.labelValues("global", storageId).set(v);
            }
        }

        if (nodeItemsByStorageId != null) {
            for (Map.Entry<String, Long> e : nodeItemsByStorageId.entrySet()) {
                String storageId = (e.getKey() != null) ? e.getKey() : "";
                long v = (e.getValue() != null) ? e.getValue() : 0L;
                storageItems.labelValues("node", storageId).set(v);
            }
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
            throw new IllegalStateException("PrometheusMetricsAgent is not started");
        }
    }
}