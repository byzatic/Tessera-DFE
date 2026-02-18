# Observability в Tessera DFE

Tessera DFE реализует слой observability через экспорт метрик в формате
Prometheus. Это обеспечивает прозрачность исполнения графа, состояние
JVM и поведение процесса на уровне ОС.

------------------------------------------------------------------------

## 1. Метрики уровня графа

### tessera_graph_execution_seconds

    # HELP tessera_graph_execution_seconds Graph execution duration in seconds
    # TYPE tessera_graph_execution_seconds gauge

Отражает общее время выполнения всего графа за цикл планирования.

Используется для: - контроля SLA выполнения - детекции деградации
производительности - capacity planning - сравнения версий графа

------------------------------------------------------------------------

## 2. Метрики уровня узла

### tessera_node_pipeline_execution_seconds

    # HELP tessera_node_pipeline_execution_seconds Node pipeline execution duration in seconds
    # TYPE tessera_node_pipeline_execution_seconds gauge

Пример:

    tessera_node_pipeline_execution_seconds{
      node_id="00143f1e-afaf-4a3d-b048-b4a9dbeb3d2b",
      node_name="pipeline_data->R_SEG",
      node_path="[ tech_object->abstract ] - [ segment->N_99 ] - [ segment_unit->pipeline ] - [ pipeline_data->R_SEG ]"
    } 0.012

Позволяет: - выявлять узкие места DAG - анализировать latency конкретных
сегментов - сравнивать узлы одного типа - диагностировать влияние
upstream-зависимостей

⚠ Включается через `publishNodePipelineExecutionTime=true`\
⚠ Увеличивает накладные расходы. Использовать для отладки.

------------------------------------------------------------------------

## 3. JVM Observability

При `jvmMetricsEnabled=true` публикуются:

### Память

-   `jvm_memory_used_bytes`
-   `jvm_memory_committed_bytes`
-   `jvm_memory_max_bytes`
-   `jvm_memory_pool_used_bytes`

### GC

-   `jvm_gc_collection_seconds`
-   `jvm_gc_collection_seconds_count`

### Потоки

-   `jvm_threads_current`
-   `jvm_threads_state`
-   `jvm_threads_deadlocked`

Позволяет анализировать heap pressure, GC pause, аллокации и состояние
потоков.

------------------------------------------------------------------------

## 4. Process-level метрики

-   `process_cpu_seconds_total`
-   `process_resident_memory_bytes`
-   `process_open_fds`
-   `process_virtual_memory_bytes`

Используются для: - контроля CPU saturation - выявления утечек памяти -
мониторинга файловых дескрипторов

------------------------------------------------------------------------

## Архитектура Observability

1.  Graph-level telemetry\
2.  Node-level telemetry\
3.  Runtime telemetry (JVM + OS)

Это позволяет: - проводить data-driven оптимизацию - выявлять
регрессии - строить алерты - планировать масштабирование

------------------------------------------------------------------------

## Рекомендации

Продакшен: - `jvmMetricsEnabled=true` -
`publishNodePipelineExecutionTime=false`

Профилирование: - `publishNodePipelineExecutionTime=true` (временно)
