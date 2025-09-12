# Инструкция по созданию сервиса для Tessera Data Flow Engine

Эта инструкция описывает, как реализовать и зарегистрировать собственный сервис (`Service`) в Tessera Engine, используя модуль `tessera-service-lib`.

---

## 📦 Зависимости Maven

Добавьте в `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>io.github.byzatic</groupId>
    <artifactId>tessera-service-lib</artifactId>
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

## 🧱 Основные интерфейсы

- `ServiceInterface` — интерфейс логики сервиса
- `ServiceFactoryInterface` — фабрика создания
- `ExecutionContextInterface` — контекст выполнения
- `ServiceConfigurationParameter` — параметры сервиса
- `HealthFlag*` — состояние здоровья

---

## 🏗️ Фабрика сервиса

Каждый сервис должен быть зарегистрирован через `@AutoService(ServiceFactoryInterface.class)`.

```java
@AutoService(ServiceFactoryInterface.class)
public class MyServiceFactory implements ServiceFactoryInterface {
    @Override
    public String getServiceType() {
        return "MyCustomService"; // используется в Global.json → services[].id_name
    }

    @Override
    public ServiceInterface create() {
        return new MyService();
    }
}
```

---

## 🔧 Реализация сервиса

```java
public class MyService implements ServiceInterface {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void init(ExecutionContextInterface context, Map<String, String> options) {
        logger.info("Initializing service with options: {}", options);
    }

    @Override
    public void execute() {
        logger.info("Running service...");
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down.");
    }
}
```

---

## 🧾 Конфигурация Global.json

```json
{
  "id_name": "MyCustomService",
  "description": "Example service",
  "options": [
    {
      "name": "storage",
      "data": "MY_DATA_STORAGE"
    },
    {
      "name": "apiURL",
      "data": "http://localhost:8080/metrics"
    }
  ]
}
```

---

## 🔄 Цикличный запуск (по cron)

Если нужно запускать сервис по расписанию — укажите:

```json
{
  "name": "cronMetricUpdateString",
  "data": "*/1 * * * * ?"
}
```

---

## ❤️ Мониторинг состояния

```java
getHealth().publishFlag(flag -> flag
    .type("export")
    .state(HealthFlagState.HEALTHY)
    .message("Metrics successfully exported"));
```

---

## 🪵 Логирование с контекстом MDC

```java
try (AutoCloseable ctx = mdcContext.use()) {
    logger.debug("Running scoped logic...");
}
```

`logback.xml`:

```xml
<pattern>%d [%thread] %-5level %logger [%X{identificationMessage}] - %msg%n</pattern>
```

---

## 📁 Структура проекта

```
my-service/
├── pom.xml
└── src/main/java/
    └── io/github/yourorg/service/
        ├── MyServiceFactory.java
        └── MyService.java
```

---

## ✅ Проверка

- [x] Класс фабрики помечен `@AutoService(ServiceFactoryInterface.class)`
- [x] Метод `getServiceType()` возвращает ID, используемый в Global.json
- [x] В логах отображаются MDC-контексты
- [x] Сервис корректно завершает `shutdown()`

---

## 🔚 Заключение

Сервисы в Tessera запускаются как самостоятельные логические агенты: они могут работать в фоне, по расписанию, или при старте системы. Используйте `AbstractService` или `ServiceInterface`, чтобы подключить свои метрики, экспорты, алерты и другие задачи.