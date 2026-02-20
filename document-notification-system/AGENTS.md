# AI Agent Guide - Document Notification System

## Architecture Overview

This is a **Hexagonal Architecture** (Ports & Adapters) system with **Domain-Driven Design** principles, implementing a document generation and notification platform.

### Service Structure
```
document-notification-system/
├── common/                    # Shared domain models & utilities
│   ├── common-domain/         # Base entities, value objects, events
│   └── common-application-api/
├── document-service/          # Core business service
├── customer-service/          # Customer management
├── generator-service/         # Document generation (placeholder)
└── infraestructure/           # Kafka, Docker configs
```

### Module Organization (Per Service)

Each service follows this structure:
- **`*-domain-core/`**: Pure domain logic, entities, value objects, domain services (NO Spring dependencies)
- **`*-application-service/`**: Use cases, ports (input/output interfaces), DTOs, helpers
- **`*-dataaccess/`**: JPA entities, repositories, adapters implementing output ports
- **`*-application-api/`**: REST controllers, exception handlers
- **`*-container/`**: Spring Boot application, bean configuration, dependency wiring
- **`*-messaging/`**: Kafka publishers/listeners (future implementation)

## Key Patterns & Conventions

### Domain Layer Rules
- **Aggregate Roots** extend `AggregateRoot<ID>` (from `common-domain`)
- **Value Objects** wrap primitives (e.g., `DocumentId`, `CustomerId` wrap `UUID`)
- **Domain Services** contain business logic that doesn't belong to entities
  - Example: `DocumentDomainServiceImpl.validateAndInitiateDocument()`
- **NO framework annotations** in domain-core (pure Java)

### Dependency Flow
```
Container → Application-API → Application-Service → Domain-Core
                                      ↓
                                  Dataaccess (adapts to domain interfaces)
```

### Bean Wiring Pattern
Domain services are manually wired in `*-container/config/BeanConfiguration.java`:
```java
@Bean
public IDocumentDomainService documentDomainServiceI() {
    return new DocumentDomainServiceImpl();
}
```
This keeps domain-core framework-agnostic.

### Repository Pattern
1. **Port Interface**: `*-application-service/ports/output/repository/I*Repository.java`
   - Works with domain entities (e.g., `Document`)
2. **Adapter Implementation**: `*-dataaccess/adapter/*RepositoryImpl.java`
   - Uses JPA repositories + mappers to convert between domain entities and JPA entities
3. **JPA Repository**: `*-dataaccess/repository/*JpaRepository.java` (Spring Data)
4. **JPA Entity**: `*-dataaccess/entity/*Entity.java` (e.g., `DocumentEntity`)

Example flow: `DocumentRepositoryImpl` → `DocumentJpaRepository` → PostgreSQL

### Mapper Strategy
- **Domain Mappers**: Convert DTOs ↔ Domain entities (in `*-application-service`)
  - Example: `IDocumentDataMapper.createDocumentCommandToDocument()`
- **Data Access Mappers**: Convert Domain entities ↔ JPA entities (in `*-dataaccess`)
  - Example: `DocumentDataAccessMapperI.documentToDocumentEntity()`
- All mappers use explicit interface + implementation pattern

### Transaction Boundaries
`@Transactional` is placed on:
- Command handlers (`DocumentCreateCommandHandler`)
- Helper classes (`DocumentCreateHelper.persistDocument()`)

## Development Workflows

### Running Services

**Document Service** (Port 8181):
```powershell
# Start PostgreSQL
docker run -d --name my-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=postgres -p 5434:5434 postgres:15

# Run service
cd document-service/document-container
mvn spring-boot:run
```

**Connection**: `jdbc:postgresql://localhost:5434/postgres?currentSchema=document`

**Customer Service**: Similar structure, check `customer-container/src/main/resources/application.yml`

### Building
```powershell
# From root
mvn clean install

# Single service
cd document-service
mvn clean install
```

### Project Structure
- Java 19
- Spring Boot 3.3.2
- Maven multi-module project
- Lombok for boilerplate reduction

## Adding New Features

### Creating a New Domain Entity
1. Add entity in `*-domain-core/entity/` extending `AggregateRoot<T>` or `BaseEntity<T>`
2. Add value objects in `*-domain-core/valueobject/`
3. Create domain events in `*-domain-core/event/`
4. Implement domain service logic in `*-domain-core/service/`

### Adding a New Use Case
1. Create Command/Response DTOs in `*-application-service/dto/`
2. Define input port interface in `*-application-service/ports/input/service/`
3. Implement helper in `*-application-service/helper/` with business orchestration
4. Create command handler with `@Transactional` to coordinate the flow
5. Wire in `*-application-api` REST controller

### Implementing Persistence
1. Create JPA entity in `*-dataaccess/entity/`
2. Create JPA repository in `*-dataaccess/repository/` (Spring Data interface)
3. Implement data access mapper in `*-dataaccess/mapper/`
4. Create adapter implementing output port in `*-dataaccess/adapter/`

## Common Value Objects (in `common-domain`)

- **`Money`**: BigDecimal wrapper with business operations (add, subtract, multiply)
- **`DocumentStatus`**: PENDING → GENERATED → SENT → CANCELLED
- **`DocumentType`**: Enum for document classification
- **ID wrappers**: `DocumentId`, `CustomerId`, `ItemId` all extend `BaseId<UUID>`

## Infrastructure

### Kafka Setup (Planned)
Docker Compose files in `infraestructure/docker-compose/`:
- `kafka_cluster.yml`: 3-broker cluster + Schema Registry
- `zookeeper.yml`
- `init_kafka.yml`

Network: `document-notification-system` bridge network

### Database Schema Initialization
SQL init scripts: `*-container/src/main/resources/init-schema.sql`
Controlled by: `spring.sql.init.mode=always` in `application.yml`

## Code Quality Conventions

- **Package structure**: Follow existing `com.document.notification.system.{service}.{layer}` pattern
- **Author tags**: Include Javadoc with `@author`, `@version`, `@since`
- **Logging**: Use Lombok's `@Slf4j`, log key business events (document created, saved)
- **Exception handling**:
  - Domain exceptions in `*-domain-core/exception/*DomainException.java`
  - Global exception handler in `*-application-api/exception/handler/`
- **DTO naming**: `Create*Command`, `Create*Response`, `*DTO`

## Important Notes

- **generator-service** is a placeholder (empty `src/main/java/`)
- **messaging modules** exist but have no Java code yet
- Services are designed for eventual Kafka communication (outbox pattern infrastructure exists)
- REST API versioning: `produces = "application/vnd.api.v1+json"`
- Commands reference file: `commands/01-document-service.txt` for Docker commands

- Javadocs include `@author`, `@version`, `@since`.
