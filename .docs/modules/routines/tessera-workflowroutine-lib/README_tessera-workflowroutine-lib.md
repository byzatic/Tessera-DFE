# `tessera-workflowroutine-lib`

**Версия:** 0.0.1  
**Часть проекта:** `tessera-workflow-toolkit`  
**Лицензия:** Apache‑2.0  

---

## 📦 Обзор

Модуль `tessera-workflowroutine-lib` предназначен для создания, конфигурации и исполнения workflow-рутин внутри Tessera Data Flow Engine. Он предоставляет:

- Контракты для написания рутин
- API взаимодействия с движком
- Механизмы конфигурации, контекста, графа и хранения

---

## 📁 Основные компоненты

### 🔹 `WorkflowRoutineInterface`

Основной интерфейс, описывающий поведение рутины (единицы исполнения логики).

```java
public interface WorkflowRoutineInterface {
    void init(ExecutionContextInterface context, List<ConfigurationParameter> configuration);
    void execute();
    void shutdown();
}
```

---

### 🔹 `AbstractWorkflowRoutine`

Базовый абстрактный класс с готовой реализацией шаблонного поведения.

---

### 🔹 `WorkflowRoutineFactoryInterface`

Фабрика для создания экземпляров рутин по идентификатору и параметрам. Используется системой для загрузки и инициализации кастомных рутин.

---

### 🔹 `ConfigurationParameter`

DTO-объект, описывающий параметр конфигурации для рутины. Использует билдер-паттерн.

---

## 📊 Execution Context API

Библиотека предоставляет детализированный API для доступа к информации о графе и окружении:

- `ExecutionContextInterface` — предоставляет доступ к:
  - узлу, пути, глобальному окружению, сторажам
- `GraphPathInterface` — путь узла в графе
- `NodeDescriptionInterface`, `PipelineExecutionInfoInterface` — описание текущей ноды и pipeline
- `StorageDescriptionInterface`, `StorageOptionInterface` — работа с хранилищами

---

## 🔧 API-интеграция

### `MCg3WorkflowRoutineApiInterface`

Контракт интеграции между движком и зарегистрированной workflow-рутиной. Используется для подключения и управления рутинами из движка.

---

## ❤️ Health Monitoring

Рутины могут публиковать состояние "здоровья":

- `HealthFlagState` — состояние
- `HealthFlagProxy` — объект-прокси для управления health-флагом
- Использует Builder-паттерн

---

## 🧪 Пример

```java
public class MyRoutine extends AbstractWorkflowRoutine {
    public void execute() {
        // работа с ExecutionContextInterface, сторажами и графом
    }
}
```

---

## 🔌 Подключение

```xml
<dependency>
  <groupId>io.github.byzatic</groupId>
  <artifactId>tessera-workflowroutine-lib</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

## 📌 Назначение

Этот модуль служит основой для построения:

- кастомных шагов обработки данных
- генерации метрик, трансформаций, агрегатов и т.п.
- динамически подключаемых плагинов в Tessera Engine

---

| Компонент                            | Назначение                                      |
|-------------------------------------|-------------------------------------------------|
| `WorkflowRoutineInterface`          | Контракт для рутины                             |
| `WorkflowRoutineFactoryInterface`   | Фабрика рутин                                   |
| `AbstractWorkflowRoutine`           | Базовый шаблон                                  |
| `ExecutionContextInterface`         | Доступ к графу и окружению                      |
| `ConfigurationParameter`            | Конфигурация рутины                             |
| `HealthFlag*`                       | Мониторинг состояния                            |
| `MCg3WorkflowRoutineApiInterface`   | API интеграции с движком                        |

---