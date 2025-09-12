# `tessera-service-lib`

**Версия:** 0.0.1  
**Часть проекта:** `tessera-workflow-toolkit`  
**Лицензия:** Apache‑2.0  

---

## 📦 Обзор

`**tessera-service-lib**` — модуль библиотеки `Tessera Workflow Toolkit`, предназначенный для описания и реализации сервисов, взаимодействующих с Tessera Engine. Он содержит ключевые интерфейсы и абстракции, необходимые для создания надёжных и расширяемых сервисов обработки и экспорта данных.

---

## 📁 Основные компоненты

### 🔹 `ServiceInterface`

Базовый интерфейс, описывающий поведение конкретного сервиса в системе.

```java
public interface ServiceInterface {
    void init(ExecutionContextInterface context, Map<String, String> options);
    void execute();
    void shutdown();
}
```

Позволяет движку инициализировать сервис, передав ему конфигурацию и контекст выполнения.

---

### 🔹 `AbstractService`

Абстрактный базовый класс, реализующий общую логику и шаблон выполнения сервиса. Упрощает создание новых реализаций сервисов.

---

### 🔹 `ServiceFactoryInterface`

Фабрика, создающая экземпляры сервисов по идентификатору и параметрам.

```java
public interface ServiceFactoryInterface {
    ServiceInterface create(String id, Map<String, String> options);
}
```

---

### 🔹 `MCg3ServiceApiInterface`

Контракт API взаимодействия между движком Tessera и зарегистрированным сервисом. Используется для расширенной интеграции и управления.

---

### 🔹 `ExecutionContextInterface`

Интерфейс, предоставляющий информацию о контексте запуска сервиса: его идентификатор, логгер, состояние и глобальные параметры.

---

### 🔹 Конфигурация: `ServiceConfigurationParameter`

DTO-класс, описывающий конфигурационные параметры сервиса (имя, тип, значение, опциональность). Построен с использованием паттерна `Builder`.

---

### 🔹 Здоровье сервиса: `HealthFlagProxy`, `HealthFlagState`

Классы, описывающие состояние здоровья сервиса, включая:

- текущее состояние (`HealthFlagState`)
- прокси-доступ с builder'ом (`HealthFlagProxy`)

---

### 🔹 Исключения: `ServiceOperationIncompleteException`

Исключение, выбрасываемое при неполном или аварийном завершении сервиса.

---

## ✅ Назначение

Этот модуль позволяет:

- Создавать надёжные, расширяемые сервисы (например, экспортеры, сборщики)
- Встраивать их в движок Tessera с помощью контрактов
- Контролировать их выполнение, состояние и конфигурацию

---

## 🧪 Пример использования

```java
public class MyService implements ServiceInterface {
    public void init(ExecutionContextInterface ctx, Map<String, String> options) { ... }
    public void execute() { ... }
    public void shutdown() { ... }
}
```

---

## 🔧 Подключение

```xml
<dependency>
  <groupId>io.github.byzatic</groupId>
  <artifactId>tessera-service-lib</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

## 📌 Итог

| Компонент             | Назначение                                  |
|------------------------|---------------------------------------------|
| `ServiceInterface`     | Базовый контракт сервиса                    |
| `AbstractService`      | Шаблон реализации                           |
| `ServiceFactoryInterface` | Фабрика сервисов                         |
| `ExecutionContextInterface` | Контекст исполнения                   |
| `MCg3ServiceApiInterface`  | Интеграция с движком Tessera           |
| `HealthFlag*`          | Мониторинг состояния                       |
| `ServiceConfigurationParameter` | Конфигурация сервиса             |

---

Библиотека служит основой для модульной архитектуры сервисов внутри системы Tessera и обеспечивает строгую интеграцию с Execution Engine.