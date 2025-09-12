# README: `pipeline.json` узла в Tessera Data Flow Engine

Файл `pipeline.json`, расположенный в директории конкретного узла, определяет **последовательность этапов обработки данных** в рамках этого узла. Каждый этап ссылается на одну или несколько workflow routine'ов (рутин).

---

## 📁 Расположение

Пример:
```
data/nodes/23638a5a-...-hardware_abstract->ram/pipeline.json
```

---

## 📄 Структура

Файл состоит из двух ключевых блоков:

### 1. `stages_consistency`

Упорядоченное перечисление этапов (stage'ей) исполнения.

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

Порядок строго определяет последовательность выполнения логики.

---

### 2. `stages_description`

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

---

## 🧩 Объяснение ключей

| Поле                         | Назначение                                                                 |
|------------------------------|----------------------------------------------------------------------------|
| `stage_id`                   | Уникальный идентификатор этапа                                            |
| `position`                   | Порядок выполнения этапа                                                  |
| `name`                       | Название workflow routine                                                 |
| `configuration_file_id`      | Путь до DSL-файла (может использовать `${NODE_PATH}` как переменную пути) |
| `description`                | Текстовое описание (опционально, используется в UI/логах)                |

---

## 📌 Особенности
- `stage_id` должен быть уникальным в пределах узла.

---

## ✅ Проверки

- Все `stage_id` из `stages_consistency` должны быть описаны в `stages_description`.
- Все пути `configuration_file_id` должны быть валидны.
- Порядок `position` должен быть непрерывным и начинаться с 0.

---

Файл `pipeline.json` определяет, **какие рутин-файлы и в каком порядке** будут запускаться внутри узла. Это ключевой файл, управляющий внутренним графом исполнения логики на уровне одного узла DAG.