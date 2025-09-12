# 📦 Shared Resources в Tessera

В Tessera Data Flow Engine `shared`-ресурсы представляют собой **общие компоненты, используемые как workflow-рутинами, так и сервисами**. Это переиспользуемые библиотеки, классы, интерфейсы и вспомогательные модули, не относящиеся строго к одной конкретной DSL-рутинe или сервису.

---

## 📁 Структура

```
modules/
├── services/
├── workflow_routines/
└── shared/
    └── sharedresources-project-common-0.0.1-jar-with-dependencies.jar
```

Каталог `shared/` содержит `.jar`‑файлы с общими зависимостями и утилитами. Примеры:

- модели и интерфейсы DTO
- обёртки над хранилищами и их сериализация
- механизмы обработки DSL
- общие парсеры, конвертеры, валидаторы
- логические движки (интерпретаторы, парсеры)

---

## 🔧 CompositeClassLoader

Класс `CompositeClassLoader` реализует объединённый classloader, который делегирует загрузку классов нескольким другим `ClassLoader`-ам (одновременно).

```java
new CompositeClassLoader(List<ClassLoader> delegates)
```

- Позволяет искать классы одновременно в workflow-routines, сервисах и shared-модулях.
- Отключает стандартное родительское делегирование, чтобы разрешать конфликты классов и загружать более приоритетные версии из `shared`.

Это необходимо для гибкости модульной загрузки, особенно если сервисы используют одни и те же типы данных.

---

## 🗃️ SharedResourcesRepository

`SharedResourcesRepository` — это управляемый singleton или thread-safe менеджер, который:

- отвечает за доступ к общим ресурсам (`shared`)
- обеспечивает кэширование/реюз объектов (если они singleton-like)
- реализует интерфейс `SharedResourcesRepositoryInterface`

```java
SharedResourcesRepositoryInterface repository = SharedResourcesRepository.getInstance(config);
```

---

## 🔄 Где используется

Shared-ресурсы автоматически доступны:

- в `WorkflowRoutine` через ExecutionContext (если загружены)
- в `Service` через соответствующие фабрики
- внутри движка при исполнении `.mcg3dsl`

---

## 🧩 Примеры использования

### Пример: общий DTO

```java
public class Metric {
    private String name;
    private double value;
}
```

Этот класс размещается в `shared` и используется:

- в сервисе экспорта
- в workflow-рутине, получающей данные

---

## ✅ Назначение

Shared‑ресурсы нужны, чтобы:

- не дублировать код между рутинами и сервисами
- избежать конфликтов при загрузке классов
- гарантировать совместимость и переиспользование

---

## 📌 Рекомендации

- Размещайте в `shared/` только **чистые, независимые, повторно используемые** компоненты
- Избегайте зависимости от ExecutionContext или Pipeline внутри `shared`
- Используйте `CompositeClassLoader` и `SharedResourcesRepository` только при необходимости

---