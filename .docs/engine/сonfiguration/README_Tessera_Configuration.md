# 📘 Инструкция по конфигурированию движка Tessera

Движок Tessera использует XML-файл конфигурации и класс `Configuration.java` для загрузки параметров исполнения, cron-планирования и системных путей.

---

## 📄 Конфигурационный файл: `example.configuration.xml`

Файл должен быть размещён в виде `example.configuration.xml` и доступен через путь, передаваемый в движок (обычно `-Dconfig.file=...`).

### 🔧 Пример:

```xml
<configuration>
  <graphCalculationCronCycle>0 */5 * * * ?</graphCalculationCronCycle>
  <temporaryWorkingDirectoryPath>/var/tmp/tessera/working</temporaryWorkingDirectoryPath>
  <rootProjectFolder>/opt/tessera/projects</rootProjectFolder>
</configuration>
```

---

## 🔍 Описание параметров

| Параметр                         | Описание                                                                 |
|----------------------------------|--------------------------------------------------------------------------|
| `graphCalculationCronCycle`     | Cron-выражение для запуска графа обработки данных                        |
| `temporaryWorkingDirectoryPath` | Каталог для временных данных и операций                                  |
| `rootProjectFolder`             | Корневой каталог всех проектов (где находится `data/`)                   |

### 🕒 Пример cron:

```
"0 */5 * * * ?" → каждые 5 минут
```

Формат cron:
```
секунда минута час день_месяца месяц день_недели
```

---

## ⚙️ Класс `Configuration.java`

Этот класс:

- Использует Apache Commons Configuration (`XMLConfiguration`)
- Загружает конфигурацию при запуске
- Предоставляет геттеры:
  - `getGraphCalculationCronCycle()`
  - `getTemporaryWorkingDirectoryPath()`
  - `getRootProjectFolder()`
- Инициализирует временные каталоги через `TempDirectory`

```java
Configurations configs = new Configurations();
XMLConfiguration configuration = configs.xml(new File(path));
this.graphCalculationCronCycle = configuration.getString("graphCalculationCronCycle");
```

---

## 🔐 Требования

- Все поля в XML должны быть заданы, иначе возможны `null`/ошибки выполнения.
- Пути должны существовать и быть доступны для записи (например, временные каталоги).
- cron-строка должна быть валидной (Quartz cron format, 6 позиций).

---

## ✅ Проверка

Перед запуском убедитесь:

- [x] `example.configuration.xml` доступен
- [x] Путь к нему передан как аргумент JVM (`-Dconfig.file=...`)
- [x] Каталоги существуют и имеют нужные права
- [x] Значения валидны (в частности cron)

---

## 📌 Использование

```bash
java -Dconfig.file=example.configuration.xml -jar tessera-engine.jar
```

---

## 🧪 Где используется

- Планировщик задач (cron)
- Сервисные и временные каталоги
- Загрузка проектов и graph pipeline’ов

---