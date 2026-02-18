# Tessera Data Flow Engine
Tessera Data Flow Engine is a modular execution system based on directed acyclic graphs (DAGs), designed to build flexible and extensible data processing pipelines. Each module (service/routine) represents an independent node that processes data and passes the result along the execution graph.

## Основные возможности
Движок предназначен для выполнения расчётов графов, которые описаны в составе проекта. В текущей версии вычисление выполняется от листьев к корню, что позволяет постепенно агрегировать результаты и передавать их вверх по структуре графа.

Кроме одноразовых вычислений, движок поддерживает работу сервисов постоянного действия. Такие сервисы, например REST-сервисы для предоставления рассчитанных данных, подгружаются в движок из проекта и хранятся в виде скомпилированных JAR-файлов. Они могут взаимодействовать с внутренним хранилищем движка, сохраняя туда данные и извлекая их при необходимости.

В проекте описывается сам граф и его узлы. Каждая нода имеет связанный с ней пайплайн, который выполняется, когда обход графа достигает этой точки. Пайплайн, в свою очередь, состоит из последовательных стадий. Каждая стадия включает несколько рутин — небольших программ, также поставляемых в виде JAR-файлов и подгружаемых движком из проекта.

Рутины внутри одной стадии запускаются асинхронно и могут выполняться параллельно. Это обеспечивает эффективное использование ресурсов и ускоряет выполнение вычислений, особенно в случае сложных графов с большим количеством узлов и зависимостей.

## Документация
- [Конфигурирование Tessera DFE](.docs%2Fengine%2Fconfiguration%2FRU_README_Tessera_Configuration.md)
- [Общая структура проекта Tessera-DFE](.docs%2Fproject%2FRU_README_Main.md)
- [Observability Tessera DFE](.docs%2Fobservability%2FRU_README_Tessera_Observability.md)

## Конфигурирование (краткий мануал)
Конфигурационный файл: `configuration.xml` по пути configurations/configuration.xml
```xml
<Configuration>
  <graphCalculationCronCycle>0 */5 * * * *</graphCalculationCronCycle>
  <projectName>MyAwesomeProject</projectName>
  <prometheusURI>http://0.0.0.0:9090/metrics</prometheusURI>
  <jvmMetricsEnabled>False</jvmMetricsEnabled>
  <publishNodePipelineExecutionTime>False</publishNodePipelineExecutionTime>
</Configuration>
```
- `graphCalculationCronCycle` - интервал выполнения расчета графа
- `projectName` - имя проекта (в данном случае MyAwesomeProject)
- `prometheusURI` - HTTP-адрес эндпоинта, на котором Tessera публикует метрики в формате Prometheus (`/metrics`).
- `jvmMetricsEnabled` - Булево значение (`True`/`False`). Включает публикацию JVM-метрик (heap, GC, threads и др.) в  Prometheus-эндпоинт.
- `publishNodePipelineExecutionTime` - Булево значение (`True`/`False`). Публикует метрику
времени выполнения узлов пайплайна. **Снижает
производительность**. Использовать только для отладки
графа.


Примечаение graphCalculationCronCycle
```text
graphCalculationCronCycle — cron schedule for graphManager.

    Format: 5 or 6 space-separated fields:
      [seconds] minutes hours day-of-month month day-of-week
    If only 5 fields are provided, the seconds field defaults to 0.

    Supported tokens per field:
      * (any), exact numbers (e.g., 5), ranges (a-b), lists (a,b,c),
      steps (*/n), and stepped ranges (a-b/n).
    Names (JAN–DEC, SUN–SAT) and Quartz-specific tokens (?, L, W, #) are NOT supported.
    Day-of-Month AND Day-of-Week must both match (AND semantics).
    Day-of-Week uses 0–6, where 0 = Sunday.

    Examples:
      */10 * * * * *    → every 10 seconds
      0 */5 * * * *     → every 5 minutes (on second 0)
      0 15 10 * * *     → 10:15:00 every day
      0 0 12 * * 1-5    → 12:00:00 Monday–Friday (0=Sun…6=Sat)

    NOTE:
      The Quartz-style value "*/10 * * * * ?" is NOT valid here (the "?" token isn’t supported).
      Use the 6-field form "*/10 * * * * *" to run every 10 seconds.
```

> Закоментируйте `dataDirectory` для использования в Docker

**(Без Docker)** Положите проект в data/projects/MyAwesomeProject \
**(C Docker)** Положите **zip** файл проекта например /home/user/Tessera-DFE/data/source/MyAwesomeProject.zip в /app/data/source_zip контейнера
```yaml
    # docker-compose 
    volumes:
      - /home/user/Tessera-DFE/data/source/:/app/data/source_zip
```

## Установка и Запуск

### Из исходников
**Требования к окружению**
- JDK: 17 (совместимость компиляции и запуска)
- Maven: 3.8+ (лучше 3.9.x)
- Доступ к репозиториям с внутренними артефактами (io.github.byzatic:*) — Sonatype Central/GitHub Packages/ваш private registry (в зависимости от публикации)
- Опционально: права на запись артефактов в target/ (по умолчанию Maven)

**Зависимости (ключевые)** \
**Внутренние библиотеки (из tessera-workflow-toolkit):** tessera-enginecommon-lib, tessera-storageapi-lib, tessera-service-lib, tessera-workflowroutine-lib (версия ${version.tessera_workflow_toolkit}=0.0.1) \
**Утилиты/библиотеки:** java-byzatic-commons (0.0.6), gson (2.11.0), commons-configuration2 (2.11.0), commons-beanutils (1.11.0), commons-lang3 (3.18.0), commons-math3 (3.6.1), java-semver (0.10.2), quartz (2.3.2) \
**Логирование:** slf4j-api (2.0.17) + logback-core/classic (1.5.18) \
**Тесты:** junit (4.13.1), mockito-core (4.11.0)

**Плагины сборки**
- maven-compiler-plugin (3.12.1) — source/target=17
- maven-jar-plugin (3.3.0) — пропишет Main-Class, Implementation-*, Specification-Version, добавит Class-Path с префиксом lib/
- Доставка зависимостей рядом с JAR:
maven-dependency-plugin (2.8) — копирует зависимости в target/lib
- Uber-JAR №1 (assembly):
maven-assembly-plugin (3.6.0) — jar-with-dependencies на фазе package
- Uber-JAR №2 (shade):
maven-shade-plugin (3.0.0) — прикрепляет shaded-артефакт, прописывает Main-Class и те же Implementation-*
- Тесты:
maven-surefire-plugin (3.0.0-M4) + провайдер surefire-junit47, maven-failsafe-plugin (3.0.0-M4) для IT-тестов
- Шаблоны/фильтрация:
templating-maven-plugin (1.0.0) и включённая фильтрация ресурсов (src/main/resources, filtering=true)


**Тестирование**
- Unit: JUnit 4 через surefire-junit47
- Integration: Failsafe (integration-test/verify)
```shell
mvn -DskipTests=false test
mvn -DskipITs=false verify
```

**Запуск** \
Проект настроен сразу на три способа доставки:

Создайте package
```shell
mvn -U -B -DskipTests package
```

**ВАРИАНТ ЗАПУСКА 1** - Тонкий JAR + папка lib
```shell
java -Dconfig.file=configurations/configuration.xml -jar target/tessera-dfe-0.1.2.jar
```
(манифест содержит Class-Path: lib/..., зависимости должны лежать в target/lib)

**ВАРИАНТ ЗАПУСКА 2** - Assembly Uber-JAR (jar-with-dependencies)
```shell
java -Dconfig.file=configurations/configuration.xml -jar target/tessera-dfe-0.1.2-jar-with-dependencies.jar
```

**ВАРИАНТ ЗАПУСКА 3** - Shaded Uber-JAR (прикреплённый артефакт)
Имя обычно в стиле: tessera-dfe-0.1.2-shaded.jar
```shell
java -Dconfig.file=configurations/configuration.xml -jar target/tessera-dfe-0.1.2-shaded.jar
```
---

### Docker

**Предварительные требования** \
Docker Engine 24+ и Docker Compose V2 (docker compose version) \
Доступ к образу: byzatic/tessera-data-flow-engine:latest \
Файловая структура проекта на хосте (создайте заранее):
```shell
./configurations/         # конфигурации движка
./data/source/            # входные данные проектов (будут видны как /app/data/source_zip)
./logs/                   # логи приложения
docker-compose.yml
```
Важно (Linux/SELinux): если на хосте включён SELinux — добавьте к томам суффикс :z (или :Z) в compose, чтобы избежать permission denied.

**Файл docker-compose.yml** \
Используйте ваш вариант (пояснения ниже):
```yaml
version: '3.7'
services:
  tessera-dfe:
    image: byzatic/tessera-data-flow-engine:latest
    container_name: tessera-data-flow-engine
    ports:
      - "8080:8080"
    volumes:
      - $PWD/configurations/:/app/configurations/
      - $PWD/data/source/:/app/data/source_zip
      - $PWD/logs/:/app/logs/
    environment:
      - GRAPH_CALCULATION_CRON_CYCLE=*/10 * * * * *
      - XMS=20m
      - XMX=8192m
      - DATA_DIR_WATCH_INTERVAL=5
    logging:
      driver: json-file
      options:
        max-file: "10"
        max-size: "10m"
    networks:
      - 93849dcd429a-tessera-dfe

networks:
  93849dcd429a-tessera-dfe:
    name: 93849dcd429a-tessera-dfe
    external: false
```
Что означает каждая секция
- ports: проброс API/UI сервиса проекта на http://localhost:8080/.
- volumes:
  - ./configurations/ -> /app/configurations/ — конфиги [configuration.xml](configurations%2Fexample.configuration.xml).
  - ./data/source/ -> /app/data/source_zip — входные данные проекта (.zip).
- ./logs/ -> /app/logs/ — логи сохраняются на хосте.
- environment:
  - GRAPH_CALCULATION_CRON_CYCLE — крон-расписание запуска расчёта графа. В примере — каждые 10 секунд.
  - XMS / XMX — минимальный/максимальный heap JVM (например, 20m и 8192m).
  - DATA_DIR_WATCH_INTERVAL — интервал опроса каталога данных (секунды).
- logging: ротация json-логов Docker (до 10 файлов по 10 МБ).
- networks: compose сам создаст внутреннюю сеть для сервиса.

Примечаение GRAPH_CALCULATION_CRON_CYCLE
```text
graphCalculationCronCycle — cron schedule for graphManager.

    Format: 5 or 6 space-separated fields:
      [seconds] minutes hours day-of-month month day-of-week
    If only 5 fields are provided, the seconds field defaults to 0.

    Supported tokens per field:
      * (any), exact numbers (e.g., 5), ranges (a-b), lists (a,b,c),
      steps (*/n), and stepped ranges (a-b/n).
    Names (JAN–DEC, SUN–SAT) and Quartz-specific tokens (?, L, W, #) are NOT supported.
    Day-of-Month AND Day-of-Week must both match (AND semantics).
    Day-of-Week uses 0–6, where 0 = Sunday.

    Examples:
      */10 * * * * *    → every 10 seconds
      0 */5 * * * *     → every 5 minutes (on second 0)
      0 15 10 * * *     → 10:15:00 every day
      0 0 12 * * 1-5    → 12:00:00 Monday–Friday (0=Sun…6=Sat)

    NOTE:
      The Quartz-style value "*/10 * * * * ?" is NOT valid here (the "?" token isn’t supported).
      Use the 6-field form "*/10 * * * * *" to run every 10 seconds.
```

**Запуск и управление**
```shell
# 1) Проверить, что каталоги существуют
mkdir -p configurations data/source logs

# 2) Запустить в фоне
docker compose up -d

# 3) Проверить статус
docker compose ps

# 4) Посмотреть логи
docker compose logs -f --tail=200

# 5) Остановить
docker compose down

# 6) Перезапустить после правок конфигов/окружения
docker compose up -d --force-recreate
```

**Альтернатива: запуск одной командой (без compose)**
```shell
docker run -d --name tessera-data-flow-engine \
  -p 8080:8080 \
  -v "$PWD/configurations/:/app/configurations/" \
  -v "$PWD/data/source/:/app/data/source_zip" \
  -v "$PWD/logs/:/app/logs/" \
  -e GRAPH_CALCULATION_CRON_CYCLE='*/10 * * * * *' \
  -e PROMETHEUS_URI='http://0.0.0.0:9090/metrics' \
  -e JVM_METRICS_ENABLED='True' \
  -e PUBLISH_NODE_PIPELINE_EXECUTION_TIME='False' \
  -e XMS=512m -e XMX=8192m \
  -e DATA_DIR_WATCH_INTERVAL=5 \
  --restart unless-stopped \
  byzatic/tessera-data-flow-engine:latest
```

**Настройка производительности**
- Подберите XMX под объём данных/графа. Пример: - XMX=2048m или - XMX=8g.
- Убедитесь, что у Docker-демона есть лимиты памяти/CPU, достаточные для контейнера (по умолчанию — без жёстких лимитов).
- Если диск медленный, логи лучше писать на отдельный том/диск (оставив маппинг ./logs).
- Интервал DATA_DIR_WATCH_INTERVAL подберите под ваш режим загрузки (низкий — быстрее реагирует, но чаще читает FS).

## Требования
**GAV:** io.github.byzatic.tessera:tessera-dfe:0.1.2 \
**Packaging:** jar \
**Main-Class:** ${project.groupId}.engine.App → io.github.byzatic.tessera.engine.App \
**Кодировки:** UTF-8 для build/reporting \
**Java:** 17 (и как source/target для компилятора)

## Вклад в проект
1. Создайте Issue
2. Сделайте форк репозитория
3. Создайте ветку feature/feature-issue-*
4. Отправьте Pull Request
Подробности см. в [CONTRIBUTING.md](CONTRIBUTING.md).

# Лицензия

Проект распространяется под лицензией [Apache-2.0](LICENSE)

# Контакты

**Author:** Svyatoslav Vlasov \
**Email:** s.vlaosv98@gmail.com  \
**GitHub:** @byzatic
