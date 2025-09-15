# Общая структура проекта Tessera DFE

Проект построен на модульной архитектуре и иерархической модели данных, основанной на концепции направленных ациклических графов (DAG).

---

## Корневая структура
Ниже представлено описание основных директорий проекта.

```
MyAwesomeProject/
├── data/
├── modules/
```

---

## `data/`

Содержит все данные и конфигурации, необходимые для выполнения и оркестрации workflow.

### Структура:

- `configuration_files/` — глобальные шаблоны и конфигурации:
- `Global.json` — ([DOCUMENTATION](project%2FREADME_Global_json.md)) описание глобальных хранилищ (`storages`) и сервисов (`services`) для всех узлов.
- `Project.json` — ([DOCUMENTATION](project%2FREADME_Project_json.md)) описание дерева узлов проекта в формате DAG. Содержит корневой узел и вложенные дочерние узлы.
- `nodes/` — конфигурации для каждого узла графа, каталог называется по `id` ноды (элемента `structure`) из Project.json

  Внутри каждого узла `nodes/**/` ([DOCUMENTATION](nodes%2FRU_README_Nodes_Structure.md)):
  - `configuration_files/` — локальные шаблоны и конфигурации:
  - `pipeline.json` — ([DOCUMENTATION](nodes%2FRU_README_Node_pipeline_json.md)) описание порядка исполнения workflow на узле.
  - `global.json` — ([DOCUMENTATION](nodes%2FRU_README_Node_global_json.md)) локальные ресурсы узла (на данном этапе только сторажи).

---

## `modules/`

Содержит скомпилированные JAR-модули, используемые в services, routines и shared.

### Структура:

- `services/` [DOCUMENTATION ?В процессе написания?] — реализации сервисов (services), подключаемые через `Global.json`:

- `workflow_routines/` [DOCUMENTATION ?В процессе написания?] — реализации рутин (routines), подключаемые через `pipeline.json`:

- `shared/` [DOCUMENTATION ?В процессе написания?] — общие библиотеки, используемые services и routines например для обмена данными

---

## Общее назначение

Каждый узел представляет собой логический элемент обработки (агрегация, хост, компонент оборудования и т.д.), конфигурация которого состоит из набора сторажей и пайплайна. Модули являются реализациями логики рутин и сервисов. Глобальные ресурсы централизованно определяются в `Global.json`.

Эта структура позволяет:

- централизованно управлять поведением DataFlow;
- переиспользовать конфигурации и модули;
- масштабировать и модифицировать pipeline-логику без изменения ядра.

Пример структуры проекта:
```
MyAwesomeProject
├── data
│   ├── Global.json
│   ├── Project.json
│   ├── configuration_files
│   │   ├── PrometheusQueryConfigurations.json
│   │   └── PrometheusQueryTemplates.txt
│   └── nodes
│       ├── 23638a5a-3fe8-4f76-93c2-342cacf7ad5d-hardware_abstract->ram
│       │   ├── configuration_files
│       │   │   ├── DataEnrichment-DataEnrichmentWorkflowRoutine.mcg3dsl
│       │   │   ├── LiftingData-GraphLiftingDataWorkflowRoutine.mcg3dsl
│       │   │   └── ProcessingStatus-ProcessingStatusWorkflowRoutine.mcg3dsl
│       │   ├── global.json
│       │   └── pipeline.json
│       ├── 23b51c9a-56ad-4e26-ae3d-b9f9856b2f67-aggregation_main->bpm_test
│       │   ├── configuration_files
│       │   │   ├── DataEnrichment-DataEnrichmentWorkflowRoutine.mcg3dsl
│       │   │   ├── LiftingData-GraphLiftingDataWorkflowRoutine.mcg3dsl
│       │   │   └── ProcessingStatus-ProcessingStatusWorkflowRoutine.mcg3dsl
│       │   ├── global.json
│       │   └── pipeline.json
│       ├── cccccccc-677b-4090-b6d2-266452269568-external_mc_exporter->selectel_1
│       │   ├── configuration_files
│       │   │   ├── DataEnrichment-DataEnrichmentWorkflowRoutine.mcg3dsl
│       │   │   ├── GetData-PrometheusGetDataWorkflowRoutine.mcg3dsl
│       │   │   └── ProcessingStatus-ProcessingStatusWorkflowRoutine.mcg3dsl
│       │   ├── global.json
│       │   └── pipeline.json
│       └── f34fe125-93fb-4914-a613-1aa0e414229c-hardware_abstract_unit->ram
│           ├── configuration_files
│           │   ├── DataEnrichment-DataEnrichmentWorkflowRoutine.mcg3dsl
│           │   ├── GetData-PrometheusGetDataWorkflowRoutine.mcg3dsl
│           │   └── ProcessingStatus-ProcessingStatusWorkflowRoutine.mcg3dsl
│           ├── global.json
│           └── pipeline.json
└── modules
    ├── services
    │   └── service-prometheus-export-0.0.1-jar-with-dependencies.jar
    ├── shared
    │   └── sharedresources-project-common-0.0.1-jar-with-dependencies.jar
    └── workflow_routines
        ├── workflowroutine-data-enrichment-0.0.1-jar-with-dependencies.jar
        ├── workflowroutine-graph-lifting-data-0.0.1-jar-with-dependencies.jar
        ├── workflowroutine-processing-status-0.0.1-jar-with-dependencies.jar
        └── workflowroutine-prometheus-get-data-0.0.1-jar-with-dependencies.jar

```

---