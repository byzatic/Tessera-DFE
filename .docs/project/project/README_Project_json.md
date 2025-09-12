# Project.json — описание структуры проекта Tessera-DFE

Файл `Project.json` описывает **иерархическую структуру узлов** проекта, построенную как **направленный ациклический граф (DAG)** с ограничениями по версиям схемы описания проекта:
- v1.0.0-SingleRootStrictNestedNodeTree - с единственным корнем и строгим вложением.

Этот файл располагается по пути:

```
MyAwesomeProject/
└── data/
    └── Project.json
```

---

## Структура файла

```json
{
  "project_config_version": "v1.0.0-SingleRootStrictNestedNodeTree",
  "project_name": "bpm_test_mcgen3",
  "structure": {
    "id": "...",
    "name": "...",
    "description": "",
    "downstream": [ ... ]
  }
}
```

### Поля:

- `project_config_version`: версия схемы описания проекта. В данном случае используется `v1.0.0-SingleRootStrictNestedNodeTree`, означающая наличие единственного корня и строгое иерархическое вложение узлов.
- `project_name`: человекочитаемое имя проекта.
- `structure`: структура графа.

---

## Структура узлов

Каждый узел определяется следующими полями:

- `id`: уникальный UUID узла.
- `name`: имя экземпляра узла.
- `description`: произвольное описание (опционально).
- `downstream`: массив дочерних узлов.

---

## Расшифровка текущей структуры примера

```
aggregation_main->bpm_test
├── aggregation_hosts->group1
│   └── host->10.174.18.251
│       ├── hardware_abstract->filesystem
│       │   └── hardware_abstract_unit->ro-root
│       ├── hardware_abstract->ram
│       │   └── hardware_abstract_unit->ram
│       ├── hardware_abstract->swap
│       │   └── hardware_abstract_unit->swap
│       └── hardware_abstract->cpu
│           └── hardware_abstract_unit->cpu
└── external_mc_exporter->bpm_test
```

- `aggregation_main`, `aggregation_hosts`: агрегационные узлы.
- `host`: узел, соответствующий физическому или виртуальному хосту.
- `hardware_abstract`: абстрактный компонент (например, CPU, RAM).
- `hardware_abstract_unit`: конкретная единица измерения компонента (например, `ro-root`, `ram`, `cpu`).
- `external_mc_exporter`: внешний источник данных.

---

## Назначение

Данный файл служит основой для генерации и исполнения пайплайнов в системе Tessera, определяя:

- структуру данных и маршруты обработки;
- иерархию ответственности узлов;
- взаимосвязь компонентов на всех уровнях.

---

## Соглашения

- Структура не содержит циклов.
- Каждому узлу соответствует папка `data/nodes/<id>-<name>/`.
- Названия узлов должны быть уникальны на уровне родителя.

---