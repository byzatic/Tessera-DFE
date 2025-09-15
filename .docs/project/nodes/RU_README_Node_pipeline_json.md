# README: `pipeline.json` узла в Tessera Data Flow Engine

Файл `pipeline.json`, расположенный в директории конкретного узла, определяет **последовательность этапов обработки данных** в рамках этого узла. Каждый этап ссылается на одну или несколько workflow routine'ов (рутин).
Это ключевой файл, управляющий внутренним графом исполнения логики на уровне одного узла DAG.
---

## Расположение

Пример:
```
data/nodes/23638a5a-...-hardware_abstract->ram/pipeline.json
```

---

## Структура

Файл состоит из двух ключевых блоков:

### `stages_consistency`

Упорядоченное перечисление этапов (stage'ей) исполнения. Порядок строго определяет последовательность выполнения логики.

```json
"stages_consistency": [
  {
    "position": 1,
    "stage_id": "LiftingData"
  },
  {
    "position": 2,
    "stage_id": "ProcessingStatus"
  },
  {
    "position": 3,
    "stage_id": "DataEnrichment"
  }
]
```
Описание ключей

| Поле                         | Назначение                                                                 |
|------------------------------|----------------------------------------------------------------------------|
| `stage_id`                   | Уникальный идентификатор этапа                                            |
| `position`                   | Порядок выполнения этапа                                                  |

---

### `stages_description`

Описание содержимого каждого этапа, включая:

- `stage_id`: ID этапа (соответствует записи из `stages_consistency`)
- `workers_description`: массив рутин с их конфигурацией

Пример:
```json
{
  "stage_id": "LiftingData",
  "workers_description": [
    {
      "name": "GraphLiftingDataWorkflowRoutine",
      "description": "graph lifting data workflow routine",
      "configuration_files": [
        {
          "description": "MCg3-WorkflowRoutine-DSL-File",
          "configuration_file_id": "${NODE_PATH}/LiftingData-GraphLiftingDataWorkflowRoutine.mcg3dsl"
        }
      ]
    }
  ]
}
```
Описание ключей

| Поле                         | Назначение                                                                                  |
|------------------------------|---------------------------------------------------------------------------------------------|
| `stage_id`                   | Уникальный идентификатор этапа                                                              |
| `workers_description`        | массив рутин с их конфигурацией                                                             |
| `name`                       | Название workflow routine                                                                   |
| `configuration_file_id`      | Путь до DSL-файла (может использовать `${NODE_PATH}` или `${PROJECT_GLOBAL_PATH}` см. ниже) |
| `description`                | Текстовое описание (опционально, используется в UI/логах)                                   |

> `stage_id` должен быть уникальным в пределах узла.

Переменные движка: 
- `${PROJECT_GLOBAL_PATH}` - переменныая, предоставляющая путь до хранилища файлов проекта (Например: `MyAwesomeProject/data/configuration_files`)
- `${NODE_PATH}` - переменныая, предоставляющая путь до хранилища файлов ноды (Например: `MyAwesomeProject/data/nodes/3e16568a-c188-45d9-a42b-cec96216b60d-NDOE001/configuration_files`)
