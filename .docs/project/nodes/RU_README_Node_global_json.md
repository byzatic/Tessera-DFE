# Настройки `global.json` узла в Tessera Data Flow Engine

Файл `global.json`, расположенный внутри директории узла в `data/nodes/**/`, определяет **локальные сторажи**, доступные исключительно (за исключением рутин чьей задачей является поднятие данных из нижестоящих нод) этому узлу при выполнении его пайплайна.

Это расширение или уточнение глобального `Global.json` (в `data/Global.json`), но в **локальной области видимости ноды**.
Обеспечивает изоляцию хранения данных на уровне конкретного узла.

---

## Расположение

Пример:

```
data/nodes/23638a5a-3fe8-4f76-93c2-342cacf7ad5d-hardware_abstract->ram/global.json
```

---

## Структура

```json
{
  "storages": [
    {
      "id_name": "LIFTING_DATA_STORAGE",
      "description": "Storage for lifted data",
      "options": []
    },
    {
      "id_name": "SELF_DATA_STORAGE",
      "description": "Storage for self data",
      "options": []
    },
    {
      "id_name": "PUBLIC_DATA_STORAGE",
      "description": "Storage for upstreams",
      "options": []
    }
  ]
}
```
Описание полей:
- `storages`: массив объектов-хранилищ, каждое из которых включает:
  - `id_name`: уникальный идентификатор хранилища внутри узла.
  - `description`: текстовое описание.
  - `options`: массив пар `key`–`value` (в данном случае — пустой).
