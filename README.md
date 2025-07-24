chec# Tessera Data Flow Engine
Tessera Data Flow Engine is a modular execution system based on directed acyclic graphs (DAGs), designed to build flexible and extensible data processing pipelines. Each module (or “tessera” tile) represents an independent node that processes data and passes the result along the execution graph.


```
src/
└── main/
    └── java/
        └── com/
            └── example/
                └── graphproject/
                    ├── domain/
                    │   ├── model/
                    │   │   ├── Project.java
                    │   │   ├── Graph.java
                    │   │   └── Node.java
                    │   │
                    │   ├── repository/
                    │   │   ├── ProjectRepositoryInterface.java
                    │   │   ├── GraphRepositoryInterface.java
                    │   │   └── NodeRepositoryInterface.java
                    │   │
                    │   ├── service/
                    │   │   ├── GraphManagerInterface.java
                    │   │   └── ServiceManagerInterface.java
                    │   │
                    │   └── business/
                    │       └── OrchestrationService.java
                    │
                    ├── application/
                    │   └── usecase/
                    │       └── ProjectOrchestrator.java
                    │
                    ├── infrastructure/
                    │   ├── persistence/
                    │   │   ├── JpaProjectRepository.java
                    │   │   ├── JpaGraphRepository.java
                    │   │   └── JpaNodeRepository.java
                    │   │
                    │   ├── service/
                    │   │   ├── DefaultGraphManager.java
                    │   │   └── DefaultServiceManager.java
                    │   │
                    │   └── config/
                    │       └── BeanConfiguration.java
                    │
                    └── adapter/
                        └── web/
                            └── ProjectController.java
```
| Путь                       | Описание                                                     |
|---------------------------|--------------------------------------------------------------|
| `domain/model`            | Сущности предметной области                                  |
| `domain/repository`       | Контракты хранилищ (интерфейсы)                              |
| `domain/service`          | Контракты менеджеров (интерфейсы)                            |
| `domain/business`         | Централизованная бизнес-логика, orchestration                |
| `application/usecase`     | Сценарии использования — связывают бизнес-логику и интерфейсы |
| `infrastructure/persistence` | JPA-реализации репозиториев                             |
| `infrastructure/service`  | Реализации интерфейсов менеджеров                            |
| `infrastructure/config`   | Конфигурация биндинга интерфейсов на реализации              |
| `adapter/web`             | REST API или другие входные точки                            |