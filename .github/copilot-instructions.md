# Copilot Instructions for Document Notification System

## Project Overview

This project is a Spring Boot application using Java, Maven, and SQL. It follows a layered architecture with clear separation between controllers, services, DTOs, and domain logic.

## Technologies

- Java 17+
- Spring Boot
- Maven
- SQL (JPA/Hibernate)
- Lombok

## Code Structure

- `com.document.notification.system.application.rest`: REST controllers (entry points for HTTP requests)
- `com.document.notification.system.ports.input.service`: Application service interfaces
- `com.document.notification.system.dto`: Data Transfer Objects (DTOs) for requests and responses
- `com.document.notification.system.domain`: Domain logic and utilities

## Coding Conventions

- Use Lombok annotations for boilerplate code (e.g., `@Slf4j`, `@Data`).
- REST controllers must use `@RestController` and map endpoints under `/documents`.
- Use DTOs for all request and response payloads.
- Service classes should be injected via constructor injection.
- Log important actions using `log.info` or `log.error` as appropriate.

## Testing

- Write unit tests for controllers, services, and utilities.
- Use Spring Boot Test for integration tests.
- Mock external dependencies in unit tests.

## Documentation

- Use JavaDoc for all public classes and methods.
- Document REST endpoints with OpenAPI/Swagger if possible.

## Versioning

- Follow semantic versioning in code and documentation.
- Update version numbers in Maven `pom.xml` as needed.

## Additional Notes

- Keep business logic out of controllers; delegate to service layer.
- Handle exceptions using Spring's exception handling mechanisms.
- Ensure all endpoints produce and consume `application/vnd.api.v1+json`.