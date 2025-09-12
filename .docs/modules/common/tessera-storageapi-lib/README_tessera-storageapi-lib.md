# `tessera-storageapi-lib`

**Версия:** 0.0.1  
**Часть проекта:** `tessera-workflow-toolkit`  
**Лицензия:** Apache‑2.0  

---

## 📦 Обзор

`**tessera-storageapi-lib**` — модуль библиотеки `Tessera Workflow Toolkit`, предназначенный для стандартизации взаимодействия с хранилищами данных в рамках Tessera Data Flow Engine.

Он предоставляет **абстракции API для хранения, извлечения и манипулирования данными**, а также универсальные DTO и интерфейсы для реализации backends (локальных, распределённых и т.п.).

---

## 📁 Основные компоненты

### 🔹 `StorageApiInterface`

Основной интерфейс API для взаимодействия с хранилищем данных.

```java
public interface StorageApiInterface {
    void put(String key, DataValueInterface value);
    Optional<DataValueInterface> get(String key);
    List<String> listKeys();
    boolean contains(String key);
    void delete(String key);
}
```

Обеспечивает базовые операции:

- `put()` — сохранить значение по ключу
- `get()` — получить значение по ключу
- `listKeys()` — вернуть список всех ключей
- `contains()` — проверить наличие ключа
- `delete()` — удалить значение

---

### 🔹 `DataValueInterface`

Базовый контракт для объектов, которые могут быть сохранены в стораже.

```java
public interface DataValueInterface extends Serializable {}
```

Все значения, сохраняемые через `StorageApiInterface`, должны реализовывать `DataValueInterface`.

---

### 🔹 `StorageItem`

Универсальный DTO-контейнер для передачи пары `key/value`:

```java
public class StorageItem {
    private String key;
    private DataValueInterface value;
}
```

---

### 🔹 `MCg3ApiOperationIncompleteException`

Кастомное исключение, сигнализирующее о частичном/некорректном выполнении операции API (например, не удалось получить значение или сохранить его).

```java
public class MCg3ApiOperationIncompleteException extends RuntimeException { ... }
```

---

## 🧩 Использование

```java
StorageApiInterface storage = ...
storage.put("myKey", new SomeDataObject());

if (storage.contains("myKey")) {
    DataValueInterface value = storage.get("myKey").orElseThrow();
}
```

---

## 🔧 Подключение

```xml
<dependency>
  <groupId>io.github.byzatic</groupId>
  <artifactId>tessera-storageapi-lib</artifactId>
  <version>0.0.1</version>
</dependency>
```

---

## 🧪 Расширение

Чтобы реализовать собственное хранилище:

1. Реализуйте `StorageApiInterface`.
2. Обеспечьте сериализацию всех `DataValueInterface` объектов.
3. Используйте `StorageItem` при трансфере данных между сервисами.

---

## 📌 Назначение

Этот модуль служит стабильной контрактной прослойкой между движком и реализациями сторажей (файловых, in-memory, сетевых и т.д.), позволяя:

- стандартизировать работу с хранилищами;
- реализовать переиспользуемую логику;
- легко тестировать обработчики данных.

---