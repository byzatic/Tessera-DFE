# `tessera-enginecommon-lib`

**Версия:** 0.0.1  
**Часть проекта:** `tessera-workflow-toolkit`  
**Лицензия:** Apache‑2.0  

##  Обзор

`**tessera-enginecommon-lib**` — это один из модулей Maven-пакета **Tessera Workflow Toolkit**, который предоставляет утилиты общего назначения для движка **Tessera Data Flow Engine**.

##  Назначение
Это библиотека общего назначения для движка.

На данный момент эта библиотека предназначена для управления **контекстом логирования (MDC — Mapped Diagnostic Context)** в рамках движка:

- Позволяет добавлять контекст (например, `type`, `identificationMessage`) в каждую лог-запись.
- Повышает трассируемость логов в распределённых и асинхронных системах.
- Обеспечивает автоматическое управление MDC-контекстом через метод `use()`.

##  Функционал

В библиотеке реализован интерфейс:

```java
package io.github.byzatic.tessera.enginecommon.logging;

public interface MdcContextInterface {
    void apply();
    void clear();
    AutoCloseable use();

    @Override boolean equals(Object o);
    @Override int hashCode();
    @Override String toString();
}
```

- `apply()` — устанавливает MDC-контекст в текущем потоке.
- `clear()` — очищает MDC-контекст.
- `use()` — возвращает `AutoCloseable`, что позволяет управлять контекстом в блоке `try-with-resources`:

```java
try (AutoCloseable ctx = myMdcContext.use()) {
  // тут выполняются операции с контекстом
}
// после этого ctx автоматически очищает предыдущий контекст
```

##  Примеры логов в движке

С применением MDC-контекста логи могут содержать дополнительные метки, улучшая информативность и читаемость. Пример шаблона для logback:

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger [%X{identificationMessage}] - %msg%n
```

Примеры логов:

```
2025-08-05 13:12:30.008 [Thread-8616] DEBUG ... [type=Engine] - Running JOB with id ...
2025-08-05 13:12:22.042 [Thread-8608] DEBUG ... [type=Service PrometheusExportService] - Scope by id 618186121 was found...
2025-08-05 13:12:30.164 [Thread-8622] DEBUG ... [type=WorkflowRoutine hardware_abstract_unit->swap::1:GetData|PrometheusGetDataWorkflowRoutine:Null] - Put value DataItem ...

```

Контекст (`type=...`) помогает быстро определить, какой компонент или рутина сгенерировали те или иные логи.

##  Итоги

| Аспект                | Описание                                                            |
|-----------------------|----------------------------------------------------------------------|
| **Пакет**             | `tessera-enginecommon-lib` (часть `tessera-workflow-toolkit`)         |
| **Назначение**        | Управление MDC‑контекстом логирования                                 |
| **Ключевой интерфейс**| `MdcContextInterface` — методы `apply()`, `clear()`, `use()`          |
| **Преимущества**      | Чистые и информативные логи, упрощённая трассировка                  |

##  Подключение

Добавьте в `pom.xml`:

```xml
<dependency>
  <groupId>io.github.byzatic</groupId>
  <artifactId>tessera-enginecommon-lib</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

Если тебе будут нужны примеры кода или настройки логгера — всегда готов помочь!