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
Базовые поля проекта:
- `project_config_version`: версия схемы описания проекта. В данном случае используется `v1.0.0-SingleRootStrictNestedNodeTree`, означающая наличие единственного корня и строгое иерархическое вложение узлов.
- `project_name`: человекочитаемое имя проекта.
- `structure`: структура графа.

Поля узлов:
- `id`: уникальный UUID узла.
- `name`: имя экземпляра узла.
- `description`: произвольное описание (опционально).
- `downstream`: массив дочерних узлов.

---

## Именование нод

Каждый узел в `Project.json` имеет два важных атрибута:

- `id`: UUID узла, **уникальный** идентификатор. Так же можно вписать специальнуй переменную движка `#NAMED` которая установит в id значение поля `name`. При использовании `#NAMED` директории должны именоваться только по `name`.
- `name`: строка в формате `<имя_экземпляра>`, например `hardware->ram`.

Директория узла в `data/nodes/` должна называться строго по шаблону: `<id>-<name>` и `<name>` в случае использования `#NAMED`.

Пример:
Project.json в преокте
```json
{
  "project_config_version": "v1.0.0-SingleRootStrictNestedNodeTree",
  "project_name": "bpm_test_mcgen3",
  "structure": {
    "id": "23b51c9a-56ad-4e26-ae3d-b9f9856b2f67",
    "name": "aggregation_main->bpm_test",
    "description": "",
    "downstream": [
      {
        "id": "cccccccc-677b-4090-b6d2-266452269568",
        "name": "external_mc_exporter->selectel_1",
        "description": "",
        "downstream": []
      },
      {
        "id": "#NAMED",
        "name": "external_mc_exporter->sse",
        "description": "",
        "downstream": []
      }
    ]
  }
}
```
Директории нод: \
Обычные
```
23b51c9a-56ad-4e26-ae3d-b9f9856b2f67-aggregation_main->bpm_test
```
```
cccccccc-677b-4090-b6d2-266452269568-external_mc_exporter->selectel_1
```
и `#NAMED`
```
external_mc_exporter->sse
```

---

## Назначение

Данный файл служит основой для генерации и исполнения пайплайнов в системе Tessera, определяя:

- структуру данных и маршруты обработки;
- иерархию ответственности узлов;
- взаимосвязь компонентов на всех уровнях.

---

## Соглашения

- Структура не содержит циклов.
- Каждому узлу соответствует папка `data/nodes/<id>-<name>/` или `data/nodes/<name>/` в случае использования `#NAMED`.

---