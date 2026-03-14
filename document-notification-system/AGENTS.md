# AI Agent Guide - Document Notification System

## 1. Vision General del Sistema

Sistema distribuido de **generacion y notificacion de documentos** construido con **Arquitectura Hexagonal** (Ports & Adapters), **Domain-Driven Design (DDD)** y patrones empresariales **SAGA**, **CQRS** y **Outbox**.

El sistema procesa solicitudes de documentos a traves de un flujo orquestado: recibe la solicitud, genera el documento (PDF/HTML), y notifica al cliente via email con el documento adjunto.

### Stack Tecnologico

| Componente     | Tecnologia                                 |
|----------------|--------------------------------------------|
| Lenguaje       | Java 19                                    |
| Framework      | Spring Boot 3.3.2                          |
| Build          | Maven (multi-modulo)                       |
| Mensajeria     | Apache Kafka (3 brokers) + Confluent Schema Registry |
| Serializacion  | Apache Avro                                |
| Base de datos  | PostgreSQL 15 (esquemas separados por servicio) |
| Email          | Spring Boot Mail (JavaMail / SMTP)         |
| Boilerplate    | Lombok                                     |
| Contenedores   | Docker Compose                             |

---

## 2. Arquitectura de Servicios

```
                              ┌─────────────────────┐
                              │   Customer Service   │
                              │      (Port 8180)     │
                              │  Schema: customer    │
                              └────────┬────────────┘
                                       │ Kafka: customer topic
                                       ▼
┌──────────────┐    REST    ┌─────────────────────────┐
│   Cliente    │ ──────────>│    Document Service      │
│   (API)      │            │    (Port 8181)           │
└──────────────┘            │    Schema: document      │
                            │    ROL: ORQUESTADOR      │
                            └──────┬──────────┬────────┘
                                   │          │
                    Kafka:         │          │  Kafka:
                generator-request  │          │  notification-request
                                   ▼          ▼
                     ┌──────────────┐  ┌──────────────────┐
                     │  Generator   │  │  Notification     │
                     │  Service     │  │  Service          │
                     │ (Port 8182)  │  │  (Port 8183)      │
                     │ Schema:      │  │  Schema:           │
                     │  generator   │  │   notification     │
                     └──────┬───────┘  └────────┬──────────┘
                            │                    │
                 Kafka:     │                    │ Kafka:
          generator-response│                    │ notification-response
                            │                    │
                            └───────┐  ┌─────────┘
                                    ▼  ▼
                            ┌─────────────────┐
                            │ Document Service │
                            │ (SAGA completa)  │
                            └─────────────────┘
```

### Roles de cada servicio

| Servicio              | Puerto | Rol                                                                                   |
|-----------------------|--------|---------------------------------------------------------------------------------------|
| **customer-service**  | 8180   | CRUD de clientes. Publica eventos de creacion a Kafka                                 |
| **document-service**  | 8181   | **Orquestador central**. Recibe peticiones REST, coordina SAGA de generacion y notificacion |
| **generator-service** | 8182   | Genera contenido de documentos (PDF/HTML) codificado en Base64                        |
| **notification-service** | 8183 | Envia notificaciones por email (JavaMail/SMTP) con documento adjunto                 |

---

## 3. Flujo Completo (Happy Path)

```
1. POST /documents -> Document Service
   |-- Valida que el cliente exista
   |-- Crea Document (status: PENDING) en BD
   |-- Crea GenerationOutboxMessage (outbox_status: STARTED, saga_status: STARTED)
   └-- Retorna CreateDocumentResponse

2. GeneratorOutboxScheduler (cada 10s)
   |-- Poll generation_outbox WHERE outbox_status = STARTED
   └-- Publica GeneratorRequestAvroModel -> Kafka topic: generator-request

3. Generator Service recibe GeneratorRequestAvroModel
   |-- IContentGenerator genera documento (PDF/HTML -> Base64)
   |-- Persiste DocumentGeneration en BD (status: GENERATION_COMPLETED)
   |-- Crea DocumentOutboxMessage con payload del contenido generado
   └-- DocumentOutboxScheduler publica GeneratorResponseAvroModel -> Kafka topic: generator-response

4. Document Service recibe GeneratorResponseAvroModel
   |-- DocumentGenerationSaga.execute()
   |-- Actualiza Document (status: GENERATED)
   |-- Crea NotificationOutboxMessage (outbox_status: STARTED, saga_status: PROCESSING)
   └-- NotificationOutboxScheduler publica NotificationRequestAvroModel -> Kafka topic: notification-request

5. Notification Service recibe NotificationRequestAvroModel
   |-- INotificationSender envia email real via JavaMail (con adjunto Base64)
   |-- Persiste DocumentNotification en BD (status: NOTIFICATION_SENT)
   |-- Crea DocumentOutboxMessage con resultado
   └-- DocumentOutboxScheduler publica NotificationResponseAvroModel -> Kafka topic: notification-response

6. Document Service recibe NotificationResponseAvroModel
   |-- DocumentNotificationSaga.execute()
   └-- Actualiza Document (status: SENT) -> Flujo completado
```

### Flujo de Compensacion (Error Path)

```
Si Generator falla:
  DocumentGenerationSaga.compensate() -> Document status: CANCELLING -> CANCELLED

Si Notification falla:
  DocumentNotificationSaga.compensate() -> Manejo de error, posible reintento
```

---

## 4. Estructura de Modulos por Servicio

Cada servicio sigue exactamente esta estructura hexagonal:

```
service/
|-- service-domain/
|   |-- service-domain-core/           # Capa DOMINIO PURO (sin Spring)
|   |   |-- entity/                    # Aggregate Roots y entidades
|   |   |-- event/                     # Eventos de dominio
|   |   |-- exception/                 # Excepciones de dominio
|   |   |-- service/                   # Servicios de dominio (logica de negocio)
|   |   └-- valueobject/              # Value Objects
|   |
|   └-- service-application-service/   # Capa APLICACION (orquestacion)
|       |-- config/                    # Configuracion (topics, propiedades)
|       |-- dto/                       # DTOs de entrada/salida
|       |-- helper/                    # Helpers que orquestan el flujo
|       |-- mapper/                    # Mappers dominio <-> DTO
|       |-- outbox/
|       |   |-- model/                 # DocumentEventPayload, DocumentOutboxMessage
|       |   └-- scheduler/            # DocumentOutboxHelper, DocumentOutboxScheduler
|       |-- ports/
|       |   |-- input/
|       |   |   |-- message/listener/  # Puertos de entrada (mensajes Kafka)
|       |   |   └-- service/           # Puertos de entrada (casos de uso)
|       |   └-- output/
|       |       |-- message/publisher/ # Puertos de salida (publicar a Kafka)
|       |       └-- repository/        # Puertos de salida (persistencia)
|       └-- saga/                      # Pasos de SAGA (solo en document-service)
|
|-- service-dataaccess/                # Capa INFRAESTRUCTURA - Persistencia
|   |-- adapter/                       # Implementaciones de puertos de repositorio
|   |-- entity/                        # Entidades JPA
|   |-- mapper/                        # Mappers dominio <-> JPA entity
|   |-- repository/                    # Spring Data JPA repositories
|   └-- outbox/document/              # Outbox: entity, repository, adapter, mapper
|
|-- service-messaging/                 # Capa INFRAESTRUCTURA - Mensajeria
|   |-- listener/                      # Kafka consumers (implementan KafkaConsumer<T>)
|   |-- publisher/kafka/               # Kafka producers (implementan puertos de publisher)
|   └-- mapper/                        # Mappers DTO <-> Avro model
|
|-- service-application-api/           # Capa INFRAESTRUCTURA - REST (solo document-service)
|   └-- rest/                          # Controllers + exception handlers
|
└-- service-container/                 # Capa INFRAESTRUCTURA - Arranque
    |-- config/BeanConfiguration.java  # Wiring manual de beans de dominio
    |-- adapter/                       # Adaptadores de infraestructura (ej: EmailNotificationSender)
    |-- *ServiceApplication.java       # @SpringBootApplication
    └-- resources/
        |-- application.yml            # Configuracion completa
        └-- init-schema.sql            # DDL de base de datos
```

### Flujo de Dependencias (Regla de Dependencia)

```
Container --> Messaging --> Application-Service --> Domain-Core
                                    ^
              Dataaccess -----------|
              Application-API ------|

REGLA: Domain-Core NUNCA depende de capas externas. Es Java puro.
```

---

## 5. Patrones Implementados en Detalle

### 5.1 Outbox Pattern

Garantiza consistencia eventual entre la escritura en BD y la publicacion a Kafka mediante una tabla outbox que actua como cola transaccional.

```
┌─────────────────────────────────────────────────────────┐
│                    MISMA TRANSACCION                     │
│  1. Guardar entidad de dominio en tabla principal        │
│  2. Guardar OutboxMessage en tabla outbox                │
│     (outbox_status = STARTED)                            │
└─────────────────────────────────────────────────────────┘
                          |
        Scheduler (cada 10s, @Scheduled)
                          |
                          v
┌─────────────────────────────────────────────────────────┐
│  3. Poll: SELECT FROM outbox WHERE status = STARTED     │
│  4. Publicar a Kafka                                     │
│  5. Callback -> UPDATE outbox SET status = COMPLETED     │
└─────────────────────────────────────────────────────────┘
```

**Componentes por servicio:**

| Componente              | Responsabilidad                                    |
|-------------------------|----------------------------------------------------|
| `DocumentOutboxHelper`  | Guardar, consultar y actualizar mensajes outbox     |
| `DocumentOutboxScheduler` | `@Scheduled` que pollea y publica mensajes STARTED |
| `DocumentOutboxMessage` | Modelo del mensaje outbox (saga_id, payload, status) |
| `DocumentEventPayload`  | Payload JSON serializado dentro del outbox message  |
| `DocumentOutboxEntity`  | Entidad JPA con `@Version` (optimistic locking)     |

**Tablas outbox existentes:**
- `document.generation_outbox` (document-service -> generator)
- `document.notification_outbox` (document-service -> notification)
- `generator.document_outbox` (generator -> document)
- `notification.document_outbox` (notification -> document)

### 5.2 SAGA Pattern (Orquestacion)

Document-service actua como **orquestador** de dos sagas secuenciales:

```
DocumentGenerationSaga implements SagaStep<GenerationResponse>
  |-- execute()    -> Documento generado exitosamente -> status: GENERATED
  └-- compensate() -> Generacion fallo -> status: CANCELLED

DocumentNotificationSaga implements SagaStep<NotificationResponse>
  |-- execute()    -> Notificacion enviada -> status: SENT
  └-- compensate() -> Notificacion fallo -> manejo de error
```

**Interfaz base:**
```java
public interface SagaStep<T> {
    void execute(T data);
    void compensate(T data);
}
```

**SAGA_NAME constante:** `"DocumentNotificationSaga"` (en `SagaConstants`)

### 5.3 Outbox Scheduler (Polling Publisher)

Cada servicio tiene un scheduler que implementa `OutboxScheduler`:

```java
@Scheduled(fixedRateString = "${service.outbox-scheduler-fixed-rate}",
           initialDelayString = "${service.outbox-scheduler-initial-delay}")
public void processOutboxMessage() {
    // 1. Buscar mensajes con outbox_status = STARTED
    // 2. Para cada mensaje: publisher.publish(message, helper::updateOutboxMessage)
    // 3. El callback actualiza outbox_status = COMPLETED
}
```

### 5.4 Idempotencia

Antes de procesar un mensaje, se verifica si ya existe un outbox message COMPLETED para el mismo `sagaId` + status:

```java
if (publishIfOutboxMessageProcessed(request, STATUS)) {
    log.info("Already processed, skipping");
    return;
}
```

---

## 6. Topicos Kafka y Modelos Avro

### Topicos

| Topic                  | Producer          | Consumer            | Avro Model                    |
|------------------------|-------------------|---------------------|-------------------------------|
| `customer`             | customer-service  | document-service    | `CustomerAvroModel`           |
| `generator-request`    | document-service  | generator-service   | `GeneratorRequestAvroModel`   |
| `generator-response`   | generator-service | document-service    | `GeneratorResponseAvroModel`  |
| `notification-request` | document-service  | notification-service| `NotificationRequestAvroModel`|
| `notification-response`| notification-service | document-service | `NotificationResponseAvroModel`|

### Campos principales de los modelos Avro

**NotificationRequestAvroModel:**
```
id, sagaId, customerId, documentId, createdAt,
documentNotificationStatus (GENERATED),
recipientId, subject, message,
fileName?, contentType?, contentBase64?,
failureMessages[]
```

**NotificationResponseAvroModel:**
```
id, sagaId, notificationId, documentId,
recipientId, createdAt,
notificationStatus (SENT | FAILED | CANCELLED),
failureMessages[]
```

**GeneratorRequestAvroModel:**
```
id, sagaId, documentId, customerId, createdAt,
documentGenerationStatus (PENDING | CANCELLED),
documentType, fileName, periodStartDate, periodEndDate,
totalAmount, deliveryAddress, documentStatus, itemCount, metadata
```

**GeneratorResponseAvroModel:**
```
id, sagaId, generatorId, customerId, documentId, createdAt,
generationStatus (COMPLETED | FAILED | CANCELLED),
fileName, contentType, contentBase64, fileSizeInBytes,
failureMessages[]
```

Los schemas `.avsc` estan en: `infraestructure/kafka/kafka-model/src/main/resources/avro/`

---

## 7. Detalle por Servicio

### 7.1 document-service (Orquestador)

**Rol:** Punto de entrada REST. Orquesta el flujo completo via SAGAs.

**Modulos:**
- `document-application-api` -- REST controller (`POST /documents`)
- `document-domain-core` -- Entidades: `Document`, `DocumentItem`, `Customer`, `Item`
- `document-application-service` -- SAGAs, outbox schedulers, helpers
- `document-dataaccess` -- Persistencia de documentos + outbox tables
- `document-messaging` -- Kafka listeners (generation response, notification response, customer) + publishers
- `document-container` -- Spring Boot app, bean wiring

**Tablas (schema: document):**
- `documents` -- Documento principal con status
- `document_address` -- Direccion (1:1)
- `document_items` -- Items del documento (1:N)
- `generation_outbox` -- Outbox para saga de generacion
- `notification_outbox` -- Outbox para saga de notificacion

**Kafka:**
- Consume: `generator-response`, `notification-response`, `customer`
- Produce: `generator-request`, `notification-request`

### 7.2 generator-service

**Rol:** Genera contenido de documentos en Base64 (PDF/HTML).

**Domain Core:**
- **Entidad:** `DocumentGeneration` (AggregateRoot) -- tracking de generacion
- **Domain Service:** `GeneratorDomainServiceImpl` -- orquesta validacion + generacion
- **IContentGenerator / ContentGeneratorImpl** -- genera contenido por tipo (PDF/HTML -> Base64)
- **Value Objects:** `GeneratedContent` (resultado con validacion Base64), `GenerationContentData`, `GenerationId`
- **Eventos:** `DocumentGeneratedEvent`, `DocumentGenerationFailedEvent`

**Tablas (schema: generator):**
- `document_generation` -- Historial de generaciones
- `document_outbox` -- Outbox para respuesta al document-service

**Kafka:**
- Consume: `generator-request` (`GeneratorRequestAvroModel`)
- Produce: `generator-response` (`GeneratorResponseAvroModel`)

### 7.3 notification-service

**Rol:** Envia notificaciones por email con documento adjunto via JavaMail.

**Domain Core (puerto):**
- **Entidad:** `DocumentNotification` (AggregateRoot) -- tracking de notificacion
- **Domain Service:** `NotificationDomainServiceImpl` -- orquesta validacion + envio
- **INotificationSender** (interfaz/puerto) -- contrato para envio de notificaciones
- **Value Objects:** `NotificationResult` (resultado con messageId), `NotificationData`, `NotificationContent`, `Recipient`, `NotificationId`, `NotificationChannel` (EMAIL)
- **Eventos:** `NotificationSentEvent`, `NotificationFailedEvent`

**Container (adaptador de infraestructura):**
- **EmailNotificationSender** -- implementacion de `INotificationSender` usando `JavaMailSender`
  - Construye email HTML con metadata
  - Adjunta documento Base64 decodificado via `MimeMessageHelper.addAttachment()`
  - Retorna `NotificationResult` con messageId real del MimeMessage

```
DDD Port/Adapter:
  domain-core:  INotificationSender (interface/puerto)
  container:    EmailNotificationSender (adaptador infraestructura con JavaMailSender)
```

**Tablas (schema: notification):**
- `document_notification` -- Historial de notificaciones
- `document_outbox` -- Outbox para respuesta al document-service

**Kafka:**
- Consume: `notification-request` (`NotificationRequestAvroModel`)
- Produce: `notification-response` (`NotificationResponseAvroModel`)

**Configuracion SMTP (application.yml):**
```yaml
spring.mail:
  host: localhost      # MailHog/Mailpit para desarrollo
  port: 1025
notification-service.mail.from: no-reply@document-notification-system.com
```

### 7.4 customer-service

**Rol:** Gestion de clientes. Publica eventos de creacion a Kafka.

**Tablas (schema: customer):**
- `customers` -- Datos del cliente

**Kafka:**
- Produce: `customer` (`CustomerAvroModel`)

---

## 8. Infraestructura Compartida

```
infraestructure/
|-- kafka/
|   |-- kafka-config-data/    # KafkaConfigData, KafkaConsumerConfigData, KafkaProducerConfigData
|   |-- kafka-consumer/       # KafkaConsumer<T> interface + KafkaConsumerConfig
|   |-- kafka-producer/       # KafkaProducer<K,V> interface + KafkaProducerConfig + KafkaProducerHelper
|   └-- kafka-model/          # Avro models auto-generados + schemas .avsc
|-- outbox/                   # OutboxScheduler interface, OutboxStatus enum, SchedulerConfig
|-- saga/                     # SagaStep<T> interface, SagaStatus enum, SagaConstants
└-- docker-compose/           # kafka_cluster.yml, zookeeper.yml, init_kafka.yml, common.yml, .env
```

### Modulos comunes

```
common/
|-- common-domain/             # Base entities, value objects, utils (SIN Spring)
|   |-- entity/                # AggregateRoot<T>, BaseEntity<T>
|   |-- events/                # DomainEvent<T>, DomainEventPublisher<T>
|   |-- exceptions/            # DomainException
|   |-- valueobject/           # DocumentId, CustomerId, ItemId, Money,
|   |                          # DocumentStatus, DocumentType, GenerationStatus,
|   |                          # DocumentNotificationStatus, GenerationDocumentStatus
|   └-- utils/                 # DateUtils, JsonSerializationUtil, MapperUtils
|
└-- common-application-api/    # ErrorDTO, GlobalExceptionHandler
```

### Enums de Estado (flujo del documento)

```
DocumentStatus:        PENDING -> GENERATED -> SENT -> CANCELLING -> CANCELLED
GenerationStatus:      GENERATION_COMPLETED | GENERATION_FAILED | GENERATION_CANCELLED
NotificationStatus:    NOTIFICATION_PENDING | NOTIFICATION_SENT | NOTIFICATION_FAILED | NOTIFICATION_CANCELLED
SagaStatus:            STARTED | PROCESSING | COMPENSATING | SUCCEEDED | FAILED | COMPENSATED
OutboxStatus:          STARTED | COMPLETED | FAILED
```

---

## 9. Convenciones de Codigo

### Paquetes

```
com.document.notification.system.{servicio}.service.{capa}.{subcapa}

Ejemplos:
  com.document.notification.system.generator.service.domain.entity
  com.document.notification.system.notification.service.dataaccess.outbox.document.adapter
  com.document.notification.system.notification.listener  (messaging layer)
```

### Patrones de nombrado

| Patron                     | Ejemplo                                    |
|----------------------------|--------------------------------------------|
| Interfaz de dominio        | `INotificationDomainService`               |
| Implementacion de dominio  | `NotificationDomainServiceImpl`            |
| Puerto de entrada          | `NotificationRequestMessageListener`       |
| Puerto de salida           | `NotificationResponseMessagePublisher`     |
| Puerto de repositorio      | `DocumentNotificationRepository`           |
| Adaptador de repositorio   | `DocumentNotificationRepositoryImpl`       |
| Mapper de dominio          | `NotificationDataMapper` / `*Impl`         |
| Mapper de data access      | `DocumentNotificationDataMapper`           |
| Mapper de messaging        | `NotificationMessagingDataMapper`          |
| Entidad JPA                | `DocumentNotificationEntity`               |
| Evento de dominio          | `NotificationSentEvent`                    |
| DTO de entrada             | `NotificationRequest`                      |
| DTO de salida              | `NotificationResponse`                     |
| Excepcion de dominio       | `NotificationDomainException`              |
| Value Object               | `NotificationResult`, `NotificationContent`|

### Reglas generales

- **Domain-core:** Java puro. Sin `@Component`, `@Service`, `@Repository`. Los beans se crean manualmente en `BeanConfiguration`
- **Lombok:** `@Getter`, `@Builder`, `@AllArgsConstructor`, `@Slf4j` en todo el proyecto
- **Transacciones:** `@Transactional` en helpers y command handlers
- **Logging:** `@Slf4j` de Lombok. Log de eventos clave de negocio
- **Javadoc:** `@author Ivan Camilo Rincon Saavedra`, `@version`, `@since`
- **API versioning:** `produces = "application/vnd.api.v1+json"`
- **Mappers:** Siempre interfaz + implementacion separada

---

## 10. Base de Datos

### Esquemas separados por servicio

| Schema       | Puerto BD | Servicio              |
|--------------|-----------|----------------------|
| `customer`   | 5434      | customer-service     |
| `document`   | 5434      | document-service     |
| `generator`  | 5434      | generator-service    |
| `notification`| 5434     | notification-service |

**Conexion:** `jdbc:postgresql://localhost:5434/postgres?currentSchema={schema}`

### Init scripts

Cada servicio tiene su `init-schema.sql` en `*-container/src/main/resources/`:
- Crea schema, enums, tablas e indices
- Controlado por `spring.sql.init.mode=always`

---

## 11. Guia de Desarrollo

### Ejecutar infraestructura

```bash
# PostgreSQL
docker run -d --name my-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=admin -p 5434:5432 postgres:15

# Kafka (desde infraestructure/docker-compose/)
docker-compose -f common.yml -f zookeeper.yml -f kafka_cluster.yml -f init_kafka.yml up -d

# MailHog (para notification-service en desarrollo)
docker run -d --name mailhog -p 1025:1025 -p 8025:8025 mailhog/mailhog
# UI de correos: http://localhost:8025
```

### Compilar

```bash
# Todo el proyecto
cd document-notification-system && mvn clean install

# Un servicio individual
cd document-notification-system/notification-service && mvn clean install
```

### Ejecutar servicios

```bash
cd customer-service/customer-container   && mvn spring-boot:run  # :8180
cd document-service/document-container   && mvn spring-boot:run  # :8181
cd generator-service/generator-container && mvn spring-boot:run  # :8182
cd notification-service/notification-container && mvn spring-boot:run  # :8183
```

### Agregar una nueva funcionalidad

**Nueva entidad de dominio:**
1. Entidad en `*-domain-core/entity/` extendiendo `AggregateRoot<T>`
2. Value objects en `*-domain-core/valueobject/`
3. Eventos en `*-domain-core/event/`
4. Domain service (interfaz + impl) en `*-domain-core/service/`
5. Bean en `*-container/config/BeanConfiguration.java`

**Nuevo caso de uso:**
1. DTOs en `*-application-service/dto/`
2. Puerto de entrada en `*-application-service/ports/input/`
3. Helper con `@Transactional` en `*-application-service/helper/`
4. Mapper en `*-application-service/mapper/`
5. REST controller en `*-application-api/rest/`

**Nueva persistencia:**
1. JPA entity en `*-dataaccess/entity/`
2. JPA repository en `*-dataaccess/repository/`
3. Mapper (interfaz + impl) en `*-dataaccess/mapper/`
4. Adapter implementando puerto en `*-dataaccess/adapter/`

**Nuevo canal de notificacion (ej: SMS):**
1. Agregar valor a `NotificationChannel` enum en domain-core
2. Crear nuevo adaptador (ej: `SmsNotificationSender`) en container
3. Usar Strategy o Factory pattern en `BeanConfiguration` para seleccionar implementacion

---

## 12. Diagrama de Dependencias entre Modulos

```
┌─────────────────────────────────────────────────────────────┐
|                     notification-container                   |
|  (SpringBoot, BeanConfig, EmailNotificationSender, SMTP)    |
|-------------------------------------------------------------|
|         |              |               |              |      |
|         v              v               v              v      |
|  notification-   notification-  notification-  notification- |
|  domain-core     application-  dataaccess     messaging      |
|  (Java puro)     service       (JPA, PG)      (Kafka, Avro) |
|                  (Spring TX)                                 |
|-------------------------------------------------------------|
|                          |                                   |
|                          v                                   |
|                    common-domain                             |
|              (BaseEntity, ValueObjects, Utils)               |
|-------------------------------------------------------------|
|                          |                                   |
|                          v                                   |
|              infraestructure modules                         |
|     (kafka-*, outbox, saga)                                  |
└─────────────────────────────────────────────────────────────┘
```

---

Ultima actualizacion: 2026-03-13
