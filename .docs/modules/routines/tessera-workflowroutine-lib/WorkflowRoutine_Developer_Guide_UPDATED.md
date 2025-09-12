# Инструкция по созданию WorkflowRoutine для Tessera Data Flow Engine

Эта инструкция описывает, как реализовать свою `WorkflowRoutine` в рамках системы **Tessera**, используя библиотеки из `tessera-workflow-toolkit`.

---

## 1. 📦 Зависимости Maven

В `pom.xml` вашего проекта добавьте зависимости:

```xml
<dependencies>
  <dependency>
    <groupId>io.github.byzatic</groupId>
    <artifactId>tessera-workflowroutine-lib</artifactId>
    <version>0.0.1</version>
  </dependency>
  <dependency>
    <groupId>io.github.byzatic</groupId>
    <artifactId>tessera-storageapi-lib</artifactId>
    <version>0.0.1</version>
  </dependency>
  <dependency>
    <groupId>io.github.byzatic</groupId>
    <artifactId>tessera-enginecommon-lib</artifactId>
    <version>0.0.1</version>
  </dependency>
  <dependency>
    <groupId>com.google.auto.service</groupId>
    <artifactId>auto-service</artifactId>
    <version>1.1.1</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

---

## 2. 🧱 Основные интерфейсы

- `WorkflowRoutineInterface` — точка входа для рутины
- `WorkflowRoutineFactoryInterface` — фабрика для создания экземпляров
- `ExecutionContextInterface` — окружение исполнения
- `StorageApiInterface` — API доступа к сторажам
- `ConfigurationParameter` — параметры из `.mcg3dsl`

---

## 3. 🏗️ Фабрика и AutoService

Каждая рутина должна быть зарегистрирована через **`@AutoService(WorkflowRoutineFactoryInterface.class)`**.

Фабрика сообщает движку:
- уникальное **имя рутины** (`getRoutineType()`),
- как создать экземпляр (`create()`).

```java
@AutoService(WorkflowRoutineFactoryInterface.class)
public class MyRoutineFactory implements WorkflowRoutineFactoryInterface {
    @Override
    public String getRoutineType() {
        return "MyCustomRoutine"; // будет использоваться в mcg3dsl
    }

    @Override
    public WorkflowRoutineInterface create() {
        return new MyRoutine();
    }
}
```

---

## 4. 🔹 Реализация рутины

```java
public class MyRoutine extends AbstractWorkflowRoutine {
    @Override
    public void execute() {
        ExecutionContextInterface ctx = getExecutionContext();
        Map<String, String> params = getResolvedParameters();

        StorageApiInterface storage = ctx.getStorage("MY_STORAGE_ID");

        // логика обработки
        storage.put("someKey", new MyData(...));
    }
}
```

---

## 5. 🧾 Пример DSL (mcg3dsl)

```dsl
routine {
  name: "Step_Export"
  type: "MyCustomRoutine" // имя из фабрики!
  config {
    someParameter: "value"
  }
}
```

---

## 6. ⚙️ Работа с ExecutionContext

Примеры:

```java
NodeDescriptionInterface node = ctx.getCurrentNode();
GraphPathInterface path = ctx.getGraphPath();
PipelineExecutionInfoInterface info = ctx.getPipelineExecutionInfo();
```

---

## 7. 📦 Работа с хранилищем

```java
StorageApiInterface storage = ctx.getStorage("PUBLIC_DATA_STORAGE");

if (storage.contains("someKey")) {
    DataValueInterface value = storage.get("someKey").orElseThrow();
    // обработка value
}
```

---

## 8. ❤️ Мониторинг здоровья (опционально)

```java
getHealth().publishFlag(flag -> flag
    .type("data-processing")
    .state(HealthFlagState.HEALTHY)
    .message("Data processed successfully")
);
```

---

## 9. 🪵 Логирование с MDC

```java
try (AutoCloseable ctxLog = mdcContext.use()) {
    logger.info("Executing step {}", stepName);
}
```

Для включения в шаблон логов:
```xml
<pattern>%d [%thread] %-5level %logger [%X{identificationMessage}] - %msg%n</pattern>
```

---

## 10. 📁 Структура проекта

```
my-routine/
├── pom.xml
└── src/main/java/
    └── io/github/yourorg/workflowroutine/
        ├── MyRoutine.java
        └── MyRoutineFactory.java
```

---

## ✅ Проверка

- [x] Класс фабрики имеет `@AutoService(WorkflowRoutineFactoryInterface.class)`
- [x] Возвращает уникальный `type`
- [x] Фабрика создаёт корректный экземпляр рутины
- [x] Все зависимости на месте

---

## 🔚 Заключение

WorkflowRoutine — это основной строительный блок Tessera Engine. Используйте шаблон `AbstractWorkflowRoutine` и `ExecutionContextInterface`, чтобы реализовать мощные и расширяемые шаги обработки данных. Обязательная регистрация через `AutoService` обеспечивает автоматическую загрузку рутин во время выполнения движка.