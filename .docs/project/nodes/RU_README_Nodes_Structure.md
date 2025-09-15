# Структура узлов в Tessera Data Flow Engine

В проекте Tessera-DFE структура узлов определяется иерархически через файл `Project.json`. Каждый узел графа представлен отдельной директорией внутри `data/nodes/` и содержит конфигурации, необходимые для выполнения логики обработки данных.

---

## Именование каталогов нод (Привязка к Project.json)

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

## Структура каталога узла

Каждая папка узла включает:

- `configuration_files/`: файлы предназначенные быть доступными в пространстве имен ноды. Так же в пайплайне позже вы можете использовать `${NODE_PATH}` как указатель на директорию `.../some_node_dir/configuration_files/`

```yaml
"configuration_files": [
  {
    "description": "MCg3-WorkflowRoutine-DSL-File",
    "configuration_file_id": "${NODE_PATH}/LiftingData-GraphLiftingDataWorkflowRoutine.mcg3dsl"
  }
]
```

- `pipeline.json`: описывает порядок/пайплайн исполнения вышеуказанных рутин в рамках одного узла.

- `global.json` (необязательный): локальные сторажи, доступные внутри DSL.

---

## Согласованность

- Все узлы, указанные в `Project.json`, **обязаны иметь соответствующую директорию** в `data/nodes/`.
- Название директории должно **точно соответствовать** шаблону `id-name`, где `name` совпадает с полем `name` узла в `Project.json` или `name` при использовании `#NAMED`.

---

## Примечания

- Связи между узлами определяются в `Project.json` через поле `downstream`.
- Сами директории не хранят информацию о связях — она извлекается из дерева `Project.json`.
- Система строго проверяет корректность структуры, особенно в режиме `SingleRootStrictNestedNodeTree`.
