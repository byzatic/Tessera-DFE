# Полное ревью Tessera-DFE

Дата ревью: 2026-08-23  
Режим: read-only review production-кода, тестов, конфигурации, Maven, Docker, CI и документации.  
Проверенный runtime-путь: `App -> EngineSupervisor -> lifecycle -> project reload -> services/graph -> pipeline -> plugins/storage`.

## Итог

У проекта разумное архитектурное направление: появились изолированные project revisions, единый reload coordinator, явный lifecycle и composition root для runtime. Однако до эксплуатационной надёжности enterprise-систем уровня Kafka проект пока не дотягивает. Главные риски находятся в shutdown/reload, конкурентном storage, валидации DAG и failure propagation.

Обозначения:

- **EMERGENT** — может привести к потере обработки, зависанию, нарушению изоляции runtime или публикации заведомо ненадёжного релиза; исправлять до production rollout.
- **WARNING** — существенный риск надёжности, масштабирования, сопровождения или эксплуатации.
- **LOW PRIORITY** — технический долг, который не является немедленным production blocker.

## EMERGENT

### 1. Публичный overload `GraphManagerFactory.create(listeners)` всегда приводит к ошибке cleanup

`GraphManagerFactory.create(JobEventListener...)` вызывает self-hosted конструктор `GraphManager`, которому не передаётся `StorageManager`. Поле `storageManager` остаётся `null`, но `runGraph()` в `finally` безусловно вызывает `clear()`, а `clear()` — `storageManager.cleanupNodeStorages()`.

Падение происходит даже при пустом графе: `return` из ветки `No root graph nodes` всё равно выполняет внешний `finally`. При непустом графе `clear()` дополнительно вызывается дважды, и второй вызов может замаскировать первоначальную ошибку.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/GraphManagerFactory.java:35`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/GraphManager.java:83`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/GraphManager.java:223`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/GraphManager.java:237`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/GraphManager.java:246`

Основной orchestration-путь вызывает overload с внешним scheduler и `StorageManager`, поэтому дефект скрыт от существующих тестов.

Рекомендация: убрать некорректный overload либо передавать `StorageManager` в оба конструктора; оставить ровно одного владельца cleanup; добавить regression test для пустого и непустого графа.

### 2. Hot reload конфигурации не является транзакционным

Рабочий lifecycle останавливается до публикации новой конфигурации. `validateCandidate()` проверяет, что XML можно прочитать, но не проверяет runtime-инварианты: семантику cron, пригодность Prometheus endpoint, возможность bind порта и полный startup нового runtime.

Некорректный кандидат способен остановить исправный runtime. После этого новый lifecycle либо не запустится, либо coordinator останется жив без active project runtime.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/runtime/EngineSupervisor.java:75`
- `src/main/java/io/github/byzatic/tessera/engine/Configuration.java:324`
- `src/main/java/io/github/byzatic/tessera/engine/Configuration.java:328`
- `src/main/java/io/github/byzatic/tessera/engine/application/runtime/ProjectReloadCoordinator.java:263`

Рекомендация: готовить полный immutable candidate context, валидировать и запускать его до публикации; менять active context одной атомарной операцией; хранить предыдущий context до подтверждения readiness.

### 3. Rollback-runtime не получает failure listener

Для обычного candidate listener устанавливается перед запуском. Для runtime, созданного в `rollback()`, listener не регистрируется. Последующая авария восстановленного runtime не будет передана coordinator, и JVM может продолжить работу с мёртвой обработкой.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/application/runtime/ProjectReloadCoordinator.java:170`
- `src/main/java/io/github/byzatic/tessera/engine/application/runtime/ProjectReloadCoordinator.java:277`

Рекомендация: централизовать создание и подготовку runtime в одном методе, который всегда регистрирует listener до `start()`.

### 4. Shutdown timeout не ограничивает полное время shutdown

Сервисы удаляются последовательно, каждый со своим grace interval. Для `N` сервисов остановка может занять сумму всех timeout. После `shutdownNow()` код не подтверждает фактическое завершение orchestration thread и продолжает очищать storage и закрывать revision.

Plugin, игнорирующий interrupt, может продолжать исполняться после cleanup и одновременно с новым runtime. Это нарушает заявленную гарантию отсутствия overlap между revisions.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/ServicesManager.java:263`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/runtime/DefaultProjectRuntime.java:100`
- `src/main/java/io/github/byzatic/tessera/engine/application/runtime/ProjectReloadCoordinator.java:140`

Рекомендация: использовать один абсолютный shutdown deadline, останавливать независимые сервисы параллельно, после force-stop повторно ждать termination и запрещать активацию нового runtime, пока старый не подтверждённо завершён.

### 5. Циклический граф может зависнуть навсегда или завершиться как пустой

Централизованной проверки DAG-инварианта нет. Полностью циклический граф имеет ноль roots и считается пустым успешным запуском. Цикл, достижимый из root, переводит узлы в `WAITING`, после чего traversal ждёт готовности детей без deadline. Graph и pipeline latches также ожидаются без timeout.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/GraphManager.java:110`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/graph_traversal/GraphTraversal.java:132`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/PipelineManager.java:331`

Рекомендация: обязательная topological validation до создания runtime, явная ошибка для non-empty graph без roots, execution deadline и диагностируемый список узлов цикла.

### 6. Storage API не обеспечивает корректную конкурентную запись

`put` реализован как `contains -> delete -> create`. Это compound action поверх `ConcurrentHashMap`, но сама последовательность не атомарна. Параллельные routines могут получить ложную ошибку, потерять запись или наблюдать промежуточное отсутствие ключа. `Storage.create()` аналогично использует неатомарный `containsKey -> put`.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageManager.java:97`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageManager.java:210`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/storage/Storage.java:58`

Рекомендация: определить контракт (`put`, `putIfAbsent`, compare-and-set), затем реализовать его одной атомарной операцией concurrent map.

### 7. Lazy storage работает некорректно

При `initializeStorageByRequest=true` global storage map изначально пуст. `searchGlobalStorage()` сначала проверяет map и выбрасывает исключение, поэтому до lazy initialization выполнение не доходит. Node storage содержит check-then-act гонку: два первых обращения могут создать разные storage instances, один из которых будет перезаписан вместе с данными.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageManager.java:123`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageManager.java:140`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/storage_manager/StorageManager.java:231`

Рекомендация: сначала проверять декларацию в repository, затем использовать `computeIfAbsent`/эквивалент с корректной трансляцией исключений.

### 8. Сервисы индексируются по `hashCode`

`serviceDescriptorMap` имеет ключ типа `Integer`, а descriptor помещается по `sd.hashCode()`. Коллизия двух разных descriptors молча удалит один сервис из конфигурации. Startup при этом может считаться успешным.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/ServicesManager.java:50`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/service_manager/ServicesManager.java:159`

Рекомендация: ключом должен быть валидированный уникальный service ID; duplicate ID должен отклонять project revision.

## WARNING

### 1. Тестовая защита не соответствует критичности runtime

Текущее состояние: 10 802 строки production Java, 1 159 строк тестов, 9 test-классов и 25 успешно проходящих тестов. Два `GraphManagerNodeRepositoryTest` идентичны, кроме package declaration.

Не покрыты ключевые отказовые сценарии:

- публичный self-hosted `GraphManager`;
- `PipelineManager` и зависшие workers;
- `OrchestrationService` и bounded shutdown;
- конкурентный `StorageManager`;
- циклы DAG;
- rollback-runtime failure;
- полный startup/load/graph/pipeline/cleanup;
- container health/startup.

JaCoCo threshold и интеграционные тесты отсутствуют. Failsafe подключён, но фактического integration suite нет.

### 2. CI может публиковать image независимо от результата тестов

Jobs `test` и `build-and-push-buildah` не связаны через `needs: test` и могут выполняться параллельно. Dockerfile дополнительно использует `-DskipTests`. Следовательно, image может быть собран и опубликован до завершения либо при падении test job.

Доказательства:

- `.github/workflows/main.yml:29`
- `.github/workflows/main.yml:125`
- `Dockerfile:17`

Кроме того, jobs разрешены только для двух GitHub actors. Проверки contributions от других участников фактически пропускаются.

### 3. Расчёт всех graph paths имеет экспоненциальную стоимость

Все root paths материализуются рекурсивно для каждого worker execution context. Diamond DAG создаёт комбинаторный рост количества путей и памяти; глубокий DAG способен вызвать `StackOverflowError`.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/graph_management/GraphPathFinderIterative.java:27`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/pipeline_manager/api_interface/execution_context/ExecutionContextFactory.java:48`

### 4. Prometheus сохраняет устаревшие series

Исчезнувшие `storage_id`, node и path не удаляются и не обнуляются после project revision reload. Alerts могут продолжать работать на значениях старой revision.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/observability/PrometheusMetricsAgent.java:129`
- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/observability/PrometheusMetricsAgent.java:164`

### 5. Configuration watcher может пропустить in-place rewrite

File signature содержит только size, mtime и fileKey. Изменение той же длины при недостаточной точности mtime и неизменном inode не будет обнаружено.

Доказательство: `src/main/java/io/github/byzatic/tessera/engine/infrastructure/configuration/PollingConfigurationFileWatcher.java:165`.

### 6. Архитектурные границы протекают

`domain` зависит от конкретных scheduler API, application exceptions и infrastructure DTO. `OrchestrationService` сам создаёт schedulers и читает статический `Configuration`. Это затрудняет изолированные тесты, замену infrastructure и поддержку нескольких engine contexts.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/domain/business/OrchestrationService.java:3`
- `src/main/java/io/github/byzatic/tessera/engine/domain/repository/CommonRepository.java:4`
- `src/main/java/io/github/byzatic/tessera/engine/domain/service/GraphManagerFactoryInterface.java:3`

### 7. Глобальная статическая конфигурация остаётся shared mutable state

`Configuration` публикует набор независимых `volatile` полей. Supervisor старается читать их только между runtime generations, но контракт держится на внешней дисциплине, а не на одном immutable snapshot. Несогласованный набор значений можно получить при появлении нового caller вне этого протокола.

Рекомендация: immutable `EngineConfiguration`, передаваемая через composition root. Старое замечание про `ApplicationMainContext` к текущей версии не относится — такого класса больше нет.

### 8. Проектная модель выдаёт mutable collections наружу

Domain objects и repository DTO возвращают внутренние lists/maps напрямую. Внешняя модификация способна рассинхронизировать repository, root list, downstream cache и execution context.

Доказательства:

- `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/project_repository/dto/NodeContainer.java:27`
- `src/main/java/io/github/byzatic/tessera/engine/domain/model/node_pipeline/NodePipeline.java:35`
- `src/main/java/io/github/byzatic/tessera/engine/domain/model/node/NodeItem.java:56`

### 9. Cleanup скрывает ошибки

В production-коде найдено 28 широких либо пустых catch-блоков для `Throwable`/`Exception`. Ошибка остановки service, удаления task или listener часто не влияет на итоговый статус `STOPPED`, хотя ресурсы могут продолжить работу.

Основной участок: `src/main/java/io/github/byzatic/tessera/engine/domain/business/OrchestrationService.java:271`.

### 10. Container и supply chain не hardened

- процесс работает от root;
- отсутствуют healthcheck/readiness;
- нет read-only filesystem и явных resource limits;
- base images и privileged Buildah image не закреплены digest;
- Dockerfile принудительно использует `linux/amd64`;
- production compose использует mutable tag `latest`.

Доказательства:

- `Dockerfile:2`
- `Dockerfile:27`
- `.github/workflows/main.yml:131`
- `docker-compose.yml:4`

## LOW PRIORITY

### 1. Build создаёт три конкурирующих формы поставки

Одновременно создаются thin JAR + `lib`, assembly JAR и shaded JAR. Это повышает вероятность тестирования одного artifact и запуска другого. Compiler использует `source/target 17`, а не `release 17`, что уже вызывает предупреждение javac.

Доказательство: `pom.xml:263`.

### 2. Стиль заявлен, но не автоматизирован

CONTRIBUTING требует Google Java Style и `mvn spotless:apply`, но Spotless отсутствует в POM. Нет Checkstyle/ArchUnit enforcement, SpotBugs/Error Prone, Maven Enforcer, dependency/SBOM gate.

Доказательство: `CONTRIBUTING.md:13`.

### 3. `StructureController` теряет исходную ошибку

Оба публичных метода ловят `Exception`, после чего выбрасывают пустой `RuntimeException` без message и cause.

Доказательство: `src/main/java/io/github/byzatic/tessera/engine/infrastructure/persistence/project_structure_controller/StructureController.java:28`.

### 4. `NodeLifecycleState` является изменяемым enum

Публичный setter меняет глобальный singleton enum state. Поле должно быть `final`, setter следует удалить.

Доказательство: `src/main/java/io/github/byzatic/tessera/engine/infrastructure/service/graph_reactor/graph_manager/graph_traversal/NodeLifecycleState.java:18`.

### 5. Документация частично расходится с кодом

Найдены несовпадения в именах Prometheus parameters, default endpoint, версиях JAR и директориях `source`/`source_zip`. Это не ломает runtime напрямую, но повышает риск ошибочного deployment.

### 6. Остаточный технический шум

Смешаны русский и английский JavaDoc, сохраняются `jpa_like_*`, опечатка `NOTSTATED`, нестандартный порядок `private final static`, длинные catch-and-wrap chains и неверная logger category в service `StorageApi`.

## Сверка дополнительного описания

Дополнительный текст содержал замечания из более старой версии проекта. Результат проверки:

| Замечание | Статус в текущем коде |
|---|---|
| `GraphManagerFactory.create(listeners)` создаёт manager без storage | **Подтверждено, добавлено в EMERGENT** |
| Слабые тесты и два дублирующихся repository test | **Подтверждено с актуальными числами; WARNING** |
| Повторная генерация UUID в `SupportNodesStructureCompressor` | **Неактуально: класса и старого DAO-пути нет** |
| `ProjectRepository` зависит от `ProjectLoaderInterface` | **Неактуально: текущий interface только расширяет `FullProjectRepository`** |
| `CommonRepository` зависит от infrastructure DTO | **Подтверждено; уже учтено** |
| `ApplicationMainContext` как lazy service locator | **Неактуально: класса нет; static Configuration остаётся** |
| Отсутствует DAG/cycle validation | **Подтверждено; уже EMERGENT** |
| Missing downstream молча игнорируется | **Частично устарело: mapper теперь отклоняет unknown downstream; repository всё ещё не должен молча фильтровать** |
| `ProjectDao` не закрывает `FileReader` | **Неактуально: класса нет** |
| `URLClassLoader` кэшируется и не закрывается | **Старый loader отсутствует; lifecycle передан `ProjectRevisionHandle`/runtime session** |
| `HUBS`/`HUB_INSTALLED` никогда не очищаются | **Частично устарело: `HUB_INSTALLED` нет, `HUBS` теперь `WeakHashMap`** |
| `GraphManager.clear()` вызывается дважды | **Подтверждено; усиливает основной дефект GraphManager** |
| Docker выполняет `package` дважды | **Неактуально: текущая команда содержит один `mvn package`** |
| Docker пропускает tests и фиксирует amd64 | **Подтверждено** |
| CI ограничен двумя actors | **Подтверждено** |

## Рекомендуемый порядок исправлений

1. Исправить оба construction paths `GraphManager`, единичное владение cleanup и regression tests.
2. Сделать project/configuration activation транзакционной и добавить readiness contract.
3. Исправить rollback failure listener.
4. Ввести один shutdown deadline и гарантировать отсутствие old/new runtime overlap.
5. Добавить обязательный DAG validator и execution deadlines.
6. Сделать storage operations и lazy initialization атомарными.
7. Заменить hash-based service registry на ID-based registry с duplicate validation.
8. Добавить integration test `load -> services -> graph -> pipeline -> storage cleanup -> reload -> rollback`.
9. Связать image publishing с успешным test job; перестать пропускать проверки для contributors.
10. Затем исправлять dependency direction, immutable configuration/model и observability semantics.

## Проверка

- `mvn test install -Dgpg.skip=true --batch-mode`: **BUILD SUCCESS**.
- Tests: **25**, failures: **0**, errors: **0**, skipped: **0**.
- Shell scripts: `bash -n` успешно.
- Production Java: **10 802 строк**, tests Java: **1 159 строк**, test files: **9**.
- Production-код в рамках ревью не изменялся.

Зелёная сборка не снимает перечисленные риски: основные дефекты находятся в сценариях, для которых тестов сейчас нет.
