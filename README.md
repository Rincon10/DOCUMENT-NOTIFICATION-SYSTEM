# DOCUMENT-NOTIFICATION-SYSTEM

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Maven](https://img.shields.io/badge/Maven-3.8-blue.svg)](https://maven.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%7C%20DDD-blueviolet.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)]()

Un sistema de notificaciones distribuido orientado a documentos, diseñado con principios de arquitecturas limpias (Clean Architecture) y prácticas de diseño como Hexagonal Architecture y Domain-Driven Design (DDD). El objetivo es servir como base sólida y extensible para ejecutar notificaciones en entornos distribuidos con buena separación de responsabilidades, alta testabilidad y capacidad de evolución.

## Tabla de contenidos

- [Visión general](#visión-general)
- [Arquitectura basada en Domain-Driven Design](#arquitectura-basada-en-domain-driven-design)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Arquitectura general del sistema](#arquitectura-general-del-sistema)
- [Arquitectura del componente Document Service](#arquitectura-del-componente-document-service)
- [Arquitectura del componente Generator Service](#arquitectura-del-componente-generator-service)
- [Arquitectura del componente Notification Service](#arquitectura-del-componente-notification-service)
- [Grafo de dependencias completo del sistema](#grafo-de-dependencias-completo-del-sistema)
- [Principios arquitectónicos aplicados](#principios-arquitectónicos-aplicados)
- [Flujo de notificación (alto nivel)](#flujo-de-notificación-alto-nivel)
- [Mecanismos contra la duplicidad](#mecanismos-contra-la-duplicidad)
- [Arquitectura en la nube (Azure)](#arquitectura-en-la-nube-azure)
- [Tecnologías y patterns recomendados](#tecnologías-y-patterns-recomendados)
- [Cómo empezar](#cómo-empezar-resumen)
- [Buenas prácticas y recomendaciones](#buenas-prácticas-y-recomendaciones)
- [Contribuir](#contribuir)

## Visión general
Este repositorio contiene el código fuente y la estructura para un sistema que produce, enruta y entrega notificaciones relacionadas con documentos (por ejemplo: creación, actualización, expiración, aprobaciones). Está pensado para ser desplegado de forma distribuida, integrándose con brokers de mensajería, colas y/o eventos y exponiendo adaptadores (API, webhook, colas) según las necesidades.

### Arquitectura basada en Domain-Driven Design
El siguiente diagrama ilustra la aplicación de los principios de Domain-Driven Design (DDD) en el sistema, mostrando cómo se organizan las capas, bounded contexts, entidades, agregados y value objects. Esta estructura garantiza que el dominio permanezca en el centro de la arquitectura, con las dependencias apuntando hacia el núcleo del negocio.


![Arquitectura DDD](docs/images/00-arquitectura-DDD.png)

## Estructura del repositorio

```
DOCUMENT-NOTIFICATION-SYSTEM/
├── docs/                                    # Diagramas de arquitectura y documentación
│   ├── images/                              # Todas las imágenes de la documentación
│   │   ├── 00-arquitectura-DDD.png
│   │   ├── 00-flujo-generarl-arquitectura.png
│   │   ├── 01-arquitectura-componente-document.png
│   │   ├── 02-dependency-graph-document.png
│   │   ├── 03-dependency-graph-generator.png
│   │   ├── 04-dependency-graph-notification.png
│   │   └── 05-dependency-graph-all.png
│   ├── 02-VARIABLES-DEPLOYMENT.md           # Referencia de variables de entorno (local y cloud)
│   ├── 03-AZURE-ESTUDIANTE-PASO-A-PASO.md   # Guía única de despliegue y escalado en Azure
│   ├── 04-DIAGRAMAS-AZURE.md                # Diagramas de la arquitectura en la nube
│   ├── BATCHTEST.md                         # Pruebas de carga en ambientes de nube
│   ├── jmeter/create-document.jmx           # Plan de JMeter para POST /documents
│   └── pruebas/                             # Resultados reales de la escalera de carga (500 / 9000 / 20000)
│       ├── 0N-<total>-Summary-.png          # Captura del Summary Report de JMeter (y Mailpit)
│       └── 0N-<total>-request.csv           # Muestras crudas exportadas por JMeter (una fila por petición)
│
├── document-notification-system/            # Proyecto principal Maven multi-módulo
│   ├── document-service/                    # Bounded Context: Gestión de documentos
│   │   ├── document-domain/
│   │   │   ├── document-domain-core/        # Entidades, agregados, value objects
│   │   │   └── document-application-service/ # Casos de uso, puertos, DTOs
│   │   ├── document-dataaccess/             # Adaptadores de persistencia (JPA)
│   │   ├── document-application-api/        # Adaptadores de entrada (REST Controllers)
│   │   ├── document-messaging/              # Adaptadores de mensajería (Kafka)
│   │   └── document-container/              # Configuración Spring Boot, composición
│   │
│   ├── generator-service/                   # Bounded Context: Generación de documentos
│   │   └── [misma estructura hexagonal]
│   │
│   ├── notification-service/                # Bounded Context: Envío de notificaciones
│   │   └── [misma estructura hexagonal]
│   │
│   └── docker-compose.yml                   # Infraestructura (PostgreSQL, Kafka, etc.)
│
├── .github/                                 # GitHub Actions y templates
├── .claude/                                 # Configuración de Claude Code
├── README.md
└── .gitignore
```

### Convenciones de nomenclatura

Cada bounded context sigue la misma estructura de capas:

| Capa | Módulo | Responsabilidad |
|------|--------|-----------------|
| **Domain** | `domain-core` | Entidades, agregados, value objects, eventos de dominio |
| **Application** | `application-service` | Casos de uso, puertos (interfaces), DTOs, command handlers |
| **Infrastructure** | `dataaccess` | Adaptadores de salida: repositorios JPA, mappers |
| **Infrastructure** | `application-api` | Adaptadores de entrada: controllers REST, exception handlers |
| **Infrastructure** | `messaging` | Adaptadores de mensajería: Kafka producers/consumers |
| **Bootstrap** | `container` | Configuración Spring Boot, inyección de dependencias |

## Arquitectura general del sistema
El siguiente diagrama muestra el flujo general de la arquitectura del sistema, ilustrando cómo los diferentes componentes interactúan entre sí en un entorno distribuido. Se puede observar la separación de responsabilidades, la comunicación entre servicios, y cómo fluyen los datos desde la entrada hasta la entrega de notificaciones.

![Flujo general de la arquitectura](docs/images/00-flujo-generarl-arquitectura.png)


### Arquitectura de dependencias

Una característica fundamental de **Domain-Driven Design (DDD)** y **Clean Architecture** es la **Regla de Dependencia**: las dependencias del código fuente deben apuntar únicamente hacia adentro, hacia las capas de más alto nivel (el dominio). Esto significa que:

- El **dominio (domain-core)** no conoce ni depende de ninguna otra capa
- La **capa de aplicación (application-service)** depende únicamente del dominio
- Los **adaptadores (dataaccess, application-api, messaging)** dependen de las capas internas, nunca al revés
- El **contenedor (container)** orquesta todas las dependencias pero el dominio permanece agnóstico de infraestructura

#### Estructura de módulos y aplicación de DDD

El proyecto está organizado en **Bounded Contexts** independientes (document-service, customer-service, generator-service), cada uno siguiendo la misma estructura de capas:

```
bounded-context/
├── domain/
│   ├── domain-core/        → Entidades, Agregados, Value Objects, Eventos de Dominio
│   └── application-service/ → Casos de Uso, Puertos (Interfaces), DTOs, Command Handlers
├── dataaccess/             → Adaptadores de salida (Repositorios JPA, Mappers de persistencia)
├── application-api/        → Adaptadores de entrada (Controllers REST, Exception Handlers)
├── messaging/              → Adaptadores de mensajería (Kafka producers/consumers)
└── container/              → Configuración Spring Boot, composición de dependencias
```

Esta estructura garantiza que:
- Las **reglas de negocio** están encapsuladas en `domain-core` sin dependencias externas
- Los **casos de uso** en `application-service` orquestan el dominio y definen puertos abstractos
- Los **adaptadores** implementan los puertos sin contaminar la lógica de negocio
- El **principio de inversión de dependencias** se cumple: las capas externas dependen de abstracciones definidas por las capas internas

#### Validación del grafo de dependencias

Para verificar que la arquitectura respeta estas reglas y que las dependencias fluyen correctamente hacia el dominio, utilizamos el [depgraph-maven-plugin](https://github.com/ferstl/depgraph-maven-plugin).

**Requisitos:**
- [Graphviz](https://graphviz.org/download/) instalado en el sistema

**Comandos para generar el grafo:**

```bash
# Genera un grafo individual por módulo
mvn com.github.ferstl:depgraph-maven-plugin:graph
```

```bash
# Genera un grafo agregado de todo el proyecto
mvn com.github.ferstl:depgraph-maven-plugin:aggregate -DcreateImage=true -DreduceEdges=false -Dscope=compile "-Dincludes=com.document.notification.system*:*"
```

El grafo resultante (ubicado en la carpeta `target/`) debe mostrar que:
- `domain-core` no tiene flechas salientes hacia otros módulos del sistema
- `application-service` solo depende de `domain-core` y `common-domain`
- Los módulos de infraestructura (`dataaccess`, `application-api`) dependen de las capas internas

![Grafo de dependencias del proyecto](docs/images/02-dependency-graph-document.png)

> **Nota:** Si el grafo muestra dependencias incorrectas (por ejemplo, `domain-core` dependiendo de `dataaccess`), es señal de una violación arquitectónica que debe corregirse para mantener la integridad del diseño DDD.


## Arquitectura del componente Document Service
El componente Document Service representa el núcleo del sistema de gestión de documentos, implementando una arquitectura hexagonal (Ports & Adapters) que garantiza la separación clara entre la lógica de negocio y los detalles de infraestructura.

El diagrama a continuación ilustra la arquitectura de alto nivel del componente, destacando:

- **Capa de Dominio (Core)**: Entidades, agregados, value objects y reglas de negocio puras relacionadas con la gestión de documentos. Esta capa es independiente de frameworks y tecnologías externas.

- **Capa de Aplicación**: Casos de uso y servicios de aplicación que orquestan las operaciones del dominio, coordinando el flujo de datos entre adaptadores y el dominio.

- **Puertos (Interfaces)**: Contratos abstractos que definen cómo el núcleo se comunica con el exterior, tanto para entradas (puertos primarios) como para salidas (puertos secundarios).

- **Adaptadores**: Implementaciones concretas de los puertos que conectan con tecnologías específicas:
  - *Adaptadores de entrada*: API REST, controladores, listeners de eventos
  - *Adaptadores de salida*: Repositorios de base de datos, clientes de mensajería, servicios externos

- **Flujos de comunicación**: Muestra cómo las peticiones fluyen desde los adaptadores de entrada, atraviesan los casos de uso, interactúan con el dominio y se comunican con sistemas externos mediante adaptadores de salida.

Esta organización permite sustituir cualquier tecnología de infraestructura sin afectar la lógica de negocio, facilitando la mantenibilidad, testabilidad y evolución del sistema.

![Arquitectura del componente Document Service](docs/images/01-arquitectura-componente-document.png)

## Arquitectura del componente Generator Service

El componente **Generator Service** es responsable de la generación de documentos y reportes, siguiendo la misma estructura hexagonal que el Document Service. Este servicio procesa solicitudes de generación de documentos, aplicando plantillas y transformando datos en formatos específicos (PDF, Word, Excel, etc.).

El diagrama de dependencias muestra cómo se organizan las capas internas del servicio:

![Grafo de dependencias del Generator Service](docs/images/03-dependency-graph-generator.png)

### Responsabilidades principales
- Generación de documentos a partir de plantillas
- Conversión entre formatos de archivo
- Procesamiento batch de reportes
- Gestión de metadatos de documentos generados

## Arquitectura del componente Notification Service

El componente **Notification Service** gestiona el envío de notificaciones a través de múltiples canales (email, SMS, push notifications, webhooks). Implementa el patrón Strategy para soportar diferentes proveedores de notificaciones y garantiza la entrega confiable mediante colas de mensajes.

El grafo de dependencias ilustra la estructura interna del servicio:

![Grafo de dependencias del Notification Service](docs/images/04-dependency-graph-notification.png)

### Responsabilidades principales
- Envío de notificaciones por múltiples canales
- Envío **exactamente una vez por saga**: reserva la fila de outbox en `NOTIFICATION_PENDING` antes de
  enviar (claim-then-send) para que redeliveries, rebalances y réplicas concurrentes no dupliquen el correo.
  Ver [Mecanismos contra la duplicidad](#mecanismos-contra-la-duplicidad)
- Gestión de preferencias de notificación de usuarios
- Plantillas de notificaciones personalizables
- Tracking de estado de entrega y reintentos

## Grafo de dependencias completo del sistema

El siguiente diagrama muestra el grafo de dependencias agregado de todo el proyecto, incluyendo las relaciones entre los tres bounded contexts (Document, Generator y Notification) y sus módulos internos:

![Grafo de dependencias completo del sistema](docs/images/05-dependency-graph-all.png)

Este grafo permite visualizar:
- La independencia entre los diferentes bounded contexts
- Las dependencias internas de cada servicio siguiendo DDD
- La relación entre los módulos de dominio, aplicación e infraestructura
- Posibles acoplamientos que deban refactorizarse

## Principios arquitectónicos aplicados

El proyecto está guiado por varias prácticas y patrones de arquitectura limpia, entre los que destacan:

- Hexagonal Architecture (Ports & Adapters)
  - Los detalles de infraestructura (bases de datos, brokers, frameworks web) están aislados detrás de puertos (interfaces) y conectados mediante adaptadores.
  - Permite sustituir implementaciones (p. ej. cambiar RabbitMQ por Kafka) sin afectar la lógica de dominio.

- Domain-Driven Design (DDD)
  - El dominio de notificaciones está modelado con entidades, agregados, value objects y bounded contexts claros.
  - Se enfatiza el lenguaje ubicuo para las reglas de negocio y los eventos de dominio.

- Clean Architecture / Onion
  - Capas concéntricas: dominio (centro) → casos de uso / aplicación → interfaces/exposición → infra.
  - Dependencias dirigidas hacia el dominio; la infraestructura depende de abstracciones del dominio.

- CQRS & Event-Driven
  - Separación entre comandos (acciones que cambian estado) y consultas (lecturas) cuando aplica.
  - Uso de eventos de dominio para propagar cambios y para sincronizar componentes distribuidos (event sourcing opcional).

- Resiliencia y distribución
  - Diseño para eventual consistency, idempotencia y manejo de fallos transitorios.
  - Estrategias de reintento, circuit breaking y compensaciones cuando aplica.


## Flujo de notificación (alto nivel)
1. Un comando o evento (p. ej. "DocumentoCreado") entra por un adaptador (API, webhook).
2. El caso de uso correspondiente procesa la lógica y delega al dominio.
3. El dominio publica eventos de dominio que son manejados por handlers que encolan mensajes o llaman adaptadores.
4. Los adaptadores de mensajería entregan las notificaciones a los consumidores interesados (colas, servicios, push, correo).
5. Mecanismos de reintento y idempotencia aseguran entrega segura en un entorno distribuido.

## Mecanismos contra la duplicidad

Kafka y el patrón outbox garantizan **al menos una vez**: cualquier mensaje puede llegar repetido (redelivery
tras un rebalance, reenvío del scheduler antes del ack, reintento de un batch fallido). Cada capa del sistema
tiene un mecanismo que convierte esa repetición en un no-op. Conviene tener claro **qué protege cada uno**,
porque ninguno por separado cubre todo.

### Inventario por servicio

| Mecanismo | Dónde | Qué resuelve | Qué NO resuelve |
|---|---|---|---|
| **Outbox + scheduler** | `*_outbox` de los 3 servicios, `*OutboxScheduler` | Que un cambio en BD y su mensaje a Kafka sean atómicos: nunca se pierde un evento | Duplicados: si el ack no llega antes del siguiente tick, la fila `STARTED` se republica |
| **Índice único sobre la saga** | `uk_generation_outbox_type_saga_id_saga_status`, `uk_notification_outbox_type_saga_id_saga_status` (document-service); `document_outbox_type_saga_id_notification_status_outbox_status` (notification-service) | Que una respuesta duplicada del generador o del notificador cree dos filas de outbox para la misma saga | Efectos secundarios que ocurren **antes** del `INSERT` (generar contenido, publicar, enviar un correo) |
| **Guarda de saga** | `DocumentGenerationSaga.execute`, `DocumentNotificationSaga.execute` | Solo actuar si la fila de outbox sigue en el estado esperado (`STARTED` / `PROCESSING`); la segunda entrega no hace nada | Carreras entre dos hilos que leen el mismo estado antes de que el primero haga commit |
| **Guarda de outbox COMPLETED** | `NotificationRequestHelperImpl`, `GenerationRequestHelperImpl` | Redeliveries que llegan cuando el ciclo ya terminó: se republica la respuesta y se sale | Redeliveries mientras el trabajo está en curso |
| **Captura de `23505`** | `NotificationRequestKafkaListener`, `DocumentOutboxHelper.updateOutboxMessage` | Que una violación de unicidad no deje al consumidor en bucle infinito de reintentos: se traga, se commitea el offset | Nada de lo que pasó antes de la colisión |
| **Clave de partición = `sagaId`** | Publicadores Kafka | Orden por saga dentro de una partición | Nada durante un rebalance: el hilo viejo sigue procesando su batch mientras el nuevo arranca |
| **Claim-then-send** | `NotificationRequestHelperImpl` + `DocumentOutboxHelper.claimDocumentOutboxMessage`, reutilizando el índice único de `document_outbox` | Que dos hilos o réplicas envíen el mismo correo. Es la **única** guarda que corre antes del efecto secundario | Un crash entre el claim y el `complete` deja la fila en `PENDING` y la saga sin respuesta (ver nota al final) |

### Lock optimista vs. lock pesimista

Los dos existen en el repo y protegen cosas distintas.

**Optimista (`@Version`).** La entidad lleva una columna `version` que Hibernate incrementa en cada `UPDATE`
y compara en el `WHERE`. Si dos transacciones leen la misma fila y ambas intentan escribir, la segunda ve que
la versión cambió y falla con `ObjectOptimisticLockingFailureException`. No bloquea a nadie: detecta la
colisión al final. Sirve para evitar el *lost update*, es decir que una escritura tardía pise a otra sin
enterarse. **No** evita que dos procesos hagan el mismo trabajo, solo que ambos lo persistan.

Dónde se usa (las cuatro entidades de outbox):

- `document-service`: `GenerationOutboxEntity`, `NotificationOutboxEntity`
- `generator-service`: `DocumentOutboxEntity`
- `notification-service`: `DocumentOutboxEntity`

**Pesimista (`PESSIMISTIC_WRITE` + `lock.timeout = -2`).** Se bloquea la fila en el momento de leerla con
`SELECT ... FOR UPDATE`. El hint `-2` lo traduce Hibernate a `SKIP LOCKED` en Postgres: en vez de esperar a que
otra transacción suelte la fila, la salta. Es lo que permite que varias réplicas del mismo scheduler hagan
polling a la vez y **se repartan** las filas `STARTED` sin procesar las mismas. El lock dura lo que dura la
transacción que lo tomó.

Dónde se usa (los finders que alimentan los schedulers):

- `document-service`: `GeneratorOutboxJpaRepository.findByTypeAndOutboxStatusAndSagaStatusIn`,
  `NotificationOutboxJpaRepository.findByTypeAndOutboxStatusAndSagaStatusIn`
- `notification-service`: `DocumentOutboxJpaRepository.findByTypeAndOutboxStatusAndNotificationStatusNot`
- `generator-service`: **no lo usa** en `DocumentOutboxJpaRepository.findByTypeAndOutboxStatus`, por eso con
  varias réplicas cada una republica las mismas filas (pendiente de alinear)

Límite común a ambos en los outbox schedulers: la publicación a Kafka es asíncrona y el callback que marca
`COMPLETED` corre **después** de que la transacción del tick cerró y soltó el lock. Si el ack tarda más que el
tick (10 s por defecto), el siguiente tick vuelve a ver la fila `STARTED` y la republica. El lock protege la
lectura concurrente, no el intervalo entre lectura y ack.

### Por qué hacía falta el claim-then-send en notification-service

Todos los mecanismos anteriores se evalúan contra filas que se escriben **después** de enviar el correo. La
secuencia original era *comprobar → enviar → insertar*, así que con dos entregas del mismo `sagaId` en hilos o
réplicas distintas ambas comprobaban "no hay fila", ambas enviaban, y la segunda chocaba con el índice único al
insertar. El `23505` se tragaba y el offset se commiteaba: **dos correos, una fila, cero errores en el log**. Es
el exceso que midió la prueba de carga (49 correos de más en 9000, 2.278 en 20000).

El fix invierte el orden y saca el envío de la transacción, **sin cambios de esquema**: reutiliza la fila de
`document_outbox` y su índice único `(type, saga_id, notification_status, outbox_status)` como claim.

1. **Claim** (`DocumentOutboxHelper.claimDocumentOutboxMessage`, transacción propia con `REQUIRES_NEW`):
   `INSERT` de la fila de outbox en `NOTIFICATION_PENDING / STARTED`. Quien gana el `INSERT` es el único
   autorizado a enviar; los demás reciben `DataIntegrityViolationException`, la ignoran y salen sin tocar el
   proveedor de correo. Antes del claim, `existsDocumentOutboxMessage` descarta redeliveries de sagas ya
   reclamadas o ya terminadas, en cualquier estado.
2. **Envío**, fuera de toda transacción. Así tampoco se retiene una conexión del pool mientras el hilo espera al
   rate limiter o a los reintentos SMTP.
3. **Complete** (`DocumentOutboxHelper.completeDocumentOutboxMessage`): la misma fila pasa de `PENDING` al estado
   final (`SENT`/`FAILED`) con el payload definitivo, sigue en `STARTED` y el scheduler la publica en el siguiente
   tick. Después se guarda el historial en `document_notification`.

El scheduler del outbox excluye las filas en `NOTIFICATION_PENDING`: son claims de envíos en curso, no
respuestas publicables.

**Limitación conocida.** Si el proceso muere entre 1 y 3, la fila queda en `PENDING`: las redeliveries se
descartan, el scheduler no la publica y el documento se queda en `GENERATED`. No hay reaper automático en esta
versión; se detecta con `SELECT * FROM notification.document_outbox WHERE notification_status = 'NOTIFICATION_PENDING' AND created_at < now() - interval '1 hour'`
y se resuelve a mano (borrar la fila y reenviar la saga, o marcarla `NOTIFICATION_FAILED` para que la saga
compense). Un scheduler que haga eso automáticamente es el siguiente paso natural.

## Arquitectura en la nube (Azure)

El sistema se despliega en **Azure Container Apps** con una cuenta Azure for Students. La guía completa paso a paso (comandos, variables, escalado y costos) está en [`docs/03-AZURE-ESTUDIANTE-PASO-A-PASO.md`](docs/03-AZURE-ESTUDIANTE-PASO-A-PASO.md); las variables de entorno en [`docs/02-VARIABLES-DEPLOYMENT.md`](docs/02-VARIABLES-DEPLOYMENT.md); y las cinco vistas completas de la arquitectura en [`docs/04-DIAGRAMAS-AZURE.md`](docs/04-DIAGRAMAS-AZURE.md).

### Vista general del despliegue

Todo vive en el resource group `dns-student-rg`, salvo Kafka: la cuenta de estudiante no permite compras de Marketplace, así que Confluent Cloud se contrata directo, fuera de Azure. El correo sale por **Azure Communication Services** (API HTTPS, sin SMTP), el proveedor por defecto de `notification-service`.

```mermaid
flowchart TB
    User(["👤 Usuario / Postman"])

    subgraph Azure["☁️ Azure — resource group: dns-student-rg"]
        ACR["📦 Azure Container Registry<br/>(imágenes Docker)"]

        subgraph Env["Container Apps Environment (red privada)"]
            DOC["document-service :8181<br/>ingress: external"]
            CUS["customer-service :8184<br/>ingress: external"]
            GEN["generator-service :8182<br/>ingress: internal"]
            NOT["notification-service :8183<br/>ingress: internal"]
        end

        PG[("🗄️ PostgreSQL Flexible Server<br/>B1ms · un schema por servicio")]

        ACS["✉️ Azure Communication Services<br/>Email API HTTPS"]
    end

    subgraph Confluent["☁️ Confluent Cloud (registro directo)"]
        KAFKA["Kafka Basic<br/>5 topics × 3 particiones"]
        SR["Schema Registry (Avro)"]
    end

    User -- "HTTPS público" --> DOC
    User -- "HTTPS público" --> CUS
    ACR -. "pull de imágenes" .-> Env
    DOC & CUS & GEN & NOT -- "JDBC + SSL" --> PG
    DOC & CUS & GEN & NOT -- "SASL_SSL" --> KAFKA
    DOC & CUS & GEN & NOT -.-> SR
    NOT -- "envía correos" --> ACS
```

Solo `document-service` y `customer-service` exponen URL pública; `generator` y `notification` viven en la red privada del environment y solo se comunican por Kafka. Toda credencial (BD, Kafka, Schema Registry, ACS) se guarda como secreto de Container Apps y las variables de entorno solo la referencian (`secretref:`).

### Flujo de negocio en la nube

```mermaid
sequenceDiagram
    actor U as Usuario
    participant D as document-service
    participant K as Kafka (Confluent)
    participant G as generator-service
    participant N as notification-service
    participant A as ACS Email (Azure)

    U->>D: POST /documents (HTTPS público)
    D->>K: evento generator-request (Avro)
    K->>G: consume
    Note over G: genera el documento
    G->>K: evento generator-response
    K->>D: consume → actualiza estado
    D->>K: evento notification-request
    K->>N: consume
    N->>A: envía el correo (API HTTPS)
    N->>K: evento notification-response
```

Si un consumidor está dormido (scale-to-zero), los eventos esperan en Kafka sin perderse: el flujo se pausa, no se rompe.

### Escalado

La regla de oro: **réplicas útiles ≤ particiones del topic** (3). El escalado puede ser **manual** (`az containerapp update --min/max-replicas`) o **automático** con reglas KEDA — concurrencia HTTP para las APIs públicas y lag de Kafka para los consumidores. El patrón outbox con locking optimista ya tolera múltiples instancias sin tocar código.

```mermaid
flowchart LR
    subgraph Topic["topic: notification-request (3 particiones)"]
        P0["partición 0"]
        P1["partición 1"]
        P2["partición 2"]
    end
    subgraph CG["consumer group: notification-service"]
        R1["réplica 1"]
        R2["réplica 2"]
        R3["réplica 3"]
    end
    P0 --> R1
    P1 --> R2
    P2 --> R3
```

#### Prueba de rendimiento: subir capacidad

El `Standard_B1ms` de la capa gratuita trae `max_connections = 50` y 1 vCPU. Como cada réplica abre un pool
de 5 conexiones, con los consumidores en 3 réplicas ya se llega al tope — y un rolling restart **duplica**
la demanda, porque la revisión vieja retiene sus conexiones mientras la nueva arranca. Por eso la BD se
sube **antes** de escalar los servicios:

```bash
# 1. Subir la BD (reinicia el servidor, ~5 min; ~859 conexiones y 2 vCPU)
az postgres flexible-server update -g dns-student-rg -n dns-student-pg \
  --sku-name Standard_D2s_v3 --tier GeneralPurpose

# 2. Requisitos previos en los 4 servicios
for s in document-service customer-service generator-service notification-service; do
  az containerapp update -g dns-student-rg -n $s \
    --set-env-vars SQL_INIT_MODE=never APP_LOG_LEVEL=WARN JPA_SHOW_SQL=false
done

# 3. Escalar. Máximo 3 réplicas en los consumidores: los topics tienen 3 particiones
#    y las réplicas de más quedan ociosas
az containerapp update -g dns-student-rg -n document-service     --min-replicas 2 --max-replicas 3
az containerapp update -g dns-student-rg -n generator-service    --min-replicas 2 --max-replicas 3
az containerapp update -g dns-student-rg -n notification-service --min-replicas 2 --max-replicas 3
```

Con la carga en marcha, el plan de JMeter y las verificaciones aguas abajo están en
[`docs/BATCHTEST.md`](docs/BATCHTEST.md); la configuración por escalones (500 → 20000) está en
[Escalera de carga con JMeter](#escalera-de-carga-con-jmeter-500--20000) y los resultados medidos en
[Resultados obtenidos](#resultados-obtenidos-9-de-septiembre-de-2026).

#### Escenario aplicado: document 2 · generator 5 · notification 5

Configuración usada en una prueba real, con las variables de
[`.env-cloud`](document-notification-system/.env-cloud). Manteniendo la BD en `B1ms`, los servicios de
5 réplicas bajan su pool a **2** para respetar el presupuesto de conexiones
(`2×3 + 1×3 + 5×2 + 5×2 = 29 ≤ 45`):

```bash
# document-service: 2 réplicas fijas
az containerapp update -g dns-student-rg -n document-service \
  --min-replicas 2 --max-replicas 2 \
  --set-env-vars APP_LOG_LEVEL=INFO

# generator-service: 5 réplicas fijas, todas corriendo
# (los topics tienen 3 particiones: solo 3 réplicas consumen, las otras 2 quedan ociosas)
az containerapp update -g dns-student-rg -n generator-service \
  --min-replicas 5 --max-replicas 5 \
  --set-env-vars APP_LOG_LEVEL=INFO SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=2

# notification-service: 5 réplicas fijas, correo hacia Mailpit (el default de este escenario:
# sin cuota, ideal para pruebas de carga — requiere Mailpit desplegado, sección siguiente)
az containerapp update -g dns-student-rg -n notification-service \
  --min-replicas 5 --max-replicas 5 \
  --set-env-vars MAIL_PROVIDER=smtp MAIL_HOST=mailpit MAIL_PORT=1025 \
    MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS_ENABLE=false MAIL_SMTP_STARTTLS_REQUIRED=false \
    MAIL_RATE_LIMIT_TOKENS=100 MAIL_RATE_LIMIT_REFILL_MS=1000 \
    APP_LOG_LEVEL=INFO SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=2

# Verificar que todas las réplicas levantaron:
for s in document-service generator-service notification-service; do
  az containerapp replica list -g dns-student-rg -n $s \
    --query "[].{name:name, state:properties.runningState}" -o table
done
```

> Para enviar correos reales, cambia el proveedor a ACS con los valores de `.env-cloud`
> (`MAIL_PROVIDER=azure`, cuota 10/hora sobre dominio administrado — comando en el paso 6 de la
> sección de Mailpit). Al terminar la prueba vuelve al modo ahorro con los comandos de
> [Apagar todo](#apagar-todo-y-volver-a-la-bd-barata).

**Cómo falla al escalar.** Los cuatro modos de fallo vistos en la práctica, con su síntoma:

| Modo de fallo | Síntoma | Arreglo |
|---|---|---|
| Rollout transitorio | 1-2 reinicios que se estabilizan en ~5 min | Ninguno — se recupera solo |
| Conexiones BD agotadas | `remaining connection slots are reserved` / `Unable to determine Dialect without JDBC metadata` | Bajar `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE`, bajar réplicas o subir la BD (`Σ réplicas × pool ≤ 45` en B1ms) |
| Revisiones zombis | Revisiones viejas activas con réplicas y 0% de tráfico reteniendo su pool contra la BD | `az containerapp revision deactivate --revision <vieja>` |
| Config faltante en la imagen | Crash loop permanente (`restartCount` en cientos), NPE en beans de configuración | Parche por env vars (relaxed binding) o reconstruir la imagen |

El diagnóstico siempre empieza igual: `az containerapp replica list` (estados y reinicios) y
`az containerapp logs show --type console` (la excepción real). Para **escalar a más réplicas** que
el escenario 2/5/5: consumidores >3 requieren más particiones en Confluent primero; recalcular el
presupuesto de conexiones o subir la BD; y verificar con `replica list` que todo quede en `Running`
con reinicios estables. Los comandos completos de cada caso están en la
[guía de Azure](docs/03-AZURE-ESTUDIANTE-PASO-A-PASO.md#cómo-falla-al-escalar-y-cómo-diagnosticarlo).

#### Correo en pruebas de carga: Mailpit

**Cuándo usarlo.** El proveedor por defecto es Azure Communication Services, y sobre un **Azure Managed
Domain** su cuota es de **5 correos/minuto y 10/hora por suscripción, no ampliable**. Una prueba de 10.000
documentos tardaría ~42 días en enviar sus correos y entretanto `notification-service` acumularía 429s.

Mailpit es un servidor SMTP de pruebas: **acepta todo, no entrega nada** y expone una UI web para
inspeccionar lo capturado. Sin cuota.

**No cambia nada del sistema salvo el último salto.** Todo el pipeline sigue igual; solo cambia a dónde
entrega `notification-service`, y es un cambio de variables de entorno, sin recompilar:

```
POST /documents → document-service → Kafka (generator-request)
                → generator-service → Kafka (notification-request)
                → notification-service ──┬─→ ACS Email     (MAIL_PROVIDER=azure, 10/hora)
                                         └─→ mailpit:1025  (MAIL_PROVIDER=smtp, sin límite)
```

Regla práctica:

| Escenario | Proveedor |
|---|---|
| Demo con correos reales (pocos) | ACS (`MAIL_PROVIDER=azure`) |
| Prueba de carga / batch | **Mailpit** (`MAIL_PROVIDER=smtp`) |
| Desarrollo local | Mailpit o Gmail vía `docker-compose` |

**1) Desplegar Mailpit.**

> ⚠️ El manifiesto [`azure/mailpit.yaml`](document-notification-system/azure/mailpit.yaml) documenta la
> configuración, pero `az containerapp create --yaml` la rechaza con
> `The JSON value could not be converted to System.Boolean` — es un bug del CLI, no del manifiesto. Usa
> los flags:

```bash
az containerapp create -g dns-student-rg -n mailpit --environment dns-student-env \
  --image docker.io/axllent/mailpit:latest \
  --target-port 8025 --ingress external --transport http \
  --cpu 0.25 --memory 0.5Gi --min-replicas 1 --max-replicas 1 \
  --env-vars MP_MAX_MESSAGES=20000
```

**2) Abrir el puerto SMTP 1025.** Los flags solo configuran un puerto (el 8025 de la UI). El 1025 que usa
`notification-service` se añade como *additional port mapping*, y eso hoy solo se puede por API:

```bash
SUB=$(az account show --query id -o tsv)
ID="/subscriptions/$SUB/resourceGroups/dns-student-rg/providers/Microsoft.App/containerApps/mailpit"
cat > patch.json <<'JSON'
{"properties":{"configuration":{"ingress":{"external":true,"targetPort":8025,"transport":"Http",
"additionalPortMappings":[{"external":false,"targetPort":1025,"exposedPort":1025}]}}}}
JSON
az rest --method patch --url "https://management.azure.com${ID}?api-version=2024-03-01" --body @patch.json
```

El `external: false` del puerto 1025 es deliberado: SMTP solo debe ser alcanzable **dentro** del
environment, nunca desde internet.

**3) Apuntar `notification-service` a Mailpit.** `MAIL_PROVIDER=smtp` es imprescindible — el default de la
aplicación es `azure`:

```bash
az containerapp update -g dns-student-rg -n notification-service \
  --set-env-vars MAIL_PROVIDER=smtp MAIL_HOST=mailpit MAIL_PORT=1025 \
    MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS_ENABLE=false MAIL_SMTP_STARTTLS_REQUIRED=false \
    MAIL_RATE_LIMIT_TOKENS=100 MAIL_RATE_LIMIT_REFILL_MS=1000
```

**4) Ver los correos.** La UI web es pública:

```bash
az containerapp show -g dns-student-rg -n mailpit --query properties.configuration.ingress.fqdn -o tsv
```

Ahí ves cada correo renderizado, sus adjuntos (el PDF generado), cabeceras y fuente crudo. Desde terminal,
la API vive en el mismo dominio:

```bash
MP="https://mailpit.<dominio>.eastus.azurecontainerapps.io"
curl -s "$MP/api/v1/info"                 # totales y stats SMTP (aceptados/rechazados)
curl -s "$MP/api/v1/messages?limit=20"    # listado, paginable con &start=
curl -s "$MP/api/v1/message/<ID>"         # un correo completo
curl -s "$MP/api/v1/search?query=texto"   # búsqueda
curl -X DELETE "$MP/api/v1/messages"      # vaciar la bandeja
```

**5) Medir la prueba.** Vacía la bandeja **justo antes** de disparar JMeter, así el conteo final es solo el
de la prueba:

```bash
curl -X DELETE "$MP/api/v1/messages"
# ... corre la prueba ...
curl -s "$MP/api/v1/info"    # SMTPAccepted vs SMTPRejected = tasa de entrega del pipeline
```

**6) Al terminar: volver a ACS y borrar Mailpit.**

```bash
az containerapp update -g dns-student-rg -n notification-service \
  --set-env-vars MAIL_PROVIDER=azure MAIL_FROM='donotreply@<guid>.azurecomm.net' \
    MAIL_RATE_LIMIT_TOKENS=10 MAIL_RATE_LIMIT_REFILL_MS=3600000
az containerapp delete -g dns-student-rg -n mailpit --yes
```

**Dos límites que conviene conocer:**

- `MP_MAX_MESSAGES` es el tope de correos retenidos. Con el valor del manifiesto (`10000`) una prueba de
  10.000 queda justo en el borde y Mailpit empieza a descartar los más viejos; por eso arriba va `20000`.
- Mailpit guarda todo en `/tmp/mailpit-*.db` **sin volumen persistente**. Si la réplica se reinicia, los
  correos se pierden. Sirve para medir durante la ventana de prueba, no como archivo.

El paso a paso completo, con la verificación de cada comando, está en la
[guía de Azure](docs/03-AZURE-ESTUDIANTE-PASO-A-PASO.md#pruebas-de-carga-mailpit-en-vez-de-correos-reales).

#### Escalera de carga con JMeter: 500 → 20000

Configuración recomendada para el plan [`docs/jmeter/create-document.jmx`](docs/jmeter/create-document.jmx)
sobre el escenario **document 3 · generator 6 · notification 6** (BD en `Standard_B4ms`). Con 3 réplicas de
`document-service` a 0.5 vCPU y pool Hikari de 5, el API atiende cómodo unas 15-20 peticiones en vuelo:
más hilos no suben el throughput, solo hacen cola y generan timeouts. Por eso la escalera crece sobre
todo en `loops` y mantiene la concurrencia acotada (`total = threads × loops`).

| Total | `threads` | `rampUp` (s) | `loops` | Duración estimada | Qué mide |
|---|---|---|---|---|---|
| 500 | 10 | 20 | 50 | 1-2 min | Calentamiento y línea base de latencia |
| 1000 | 20 | 30 | 50 | 2-3 min | Primer nivel de concurrencia real |
| 2000 | 20 | 40 | 100 | 4-6 min | Estabilidad sostenida con la misma concurrencia |
| 5000 | 25 | 60 | 200 | 10-15 min | Presión sobre el pipeline Kafka y el outbox |
| 9000 | 30 | 90 | 300 | 20-30 min | Corrida objetivo |
| 20000 | 40 | 120 | 500 | 45-60 min | Techo con 3 réplicas de `document-service`; exige `MP_MAX_MESSAGES` ≥ 30000 y espera de drenaje de 60 min |

La duración real depende de la latencia que midas en el primer escalón: con p95 de 500 ms y 20 hilos el
sistema rinde ~40 peticiones/s. En la [corrida real](#resultados-obtenidos-9-de-septiembre-de-2026) las
duraciones fueron mucho menores que estas estimaciones (9000 en 2,5 min y 20000 en 3,5 min), así que
tómalas como cota superior.

**Parámetros fijos en todos los escalones:**

- `thinkTime=0` — la pausa aleatoria de hasta 100 ms que ya trae el plan basta para desincronizar hilos.
- `connectTimeout=10000` y `responseTimeout=60000` — bajar el de respuesta cuenta como error peticiones
  que el backend sí procesó.
- Mismo `customerId` semilla (`550e8400-e29b-41d4-a716-446655440001`).
- Heap de JMeter para 9000 muestras: `export HEAP="-Xms1g -Xmx2g"` antes de lanzar.

**Dos reglas entre escalones:** (1) esperar a que Mailpit alcance el total del escalón — si no, el
siguiente arranca con backlog en el outbox y en Kafka y mide dos cargas mezcladas; (2) vaciar la bandeja
para que el conteo del siguiente sea limpio. Este script aplica la escalera completa con ambas reglas:

```bash
BASE_URL="https://document-service.<dominio>.azurecontainerapps.io"
MP="https://mailpit.<dominio>.azurecontainerapps.io"
CUSTOMER_ID=550e8400-e29b-41d4-a716-446655440001
export HEAP="-Xms1g -Xmx2g"

# total:threads:rampUp:loops
for step in 500:10:20:50 1000:20:30:50 2000:20:40:100 5000:25:60:200 9000:30:90:300; do
  IFS=: read total threads ramp loops <<< "$step"
  echo "=== Escalón $total ($threads x $loops, rampUp $ramp s) ==="
  curl -s -X DELETE "$MP/api/v1/messages" > /dev/null
  rm -rf "target/jmeter/$total"
  jmeter -n -t docs/jmeter/create-document.jmx \
    -JbaseUrl="$BASE_URL" -JcustomerId="$CUSTOMER_ID" \
    -Jthreads=$threads -JrampUp=$ramp -Jloops=$loops \
    -l "target/jmeter/$total/results.jtl" -e -o "target/jmeter/$total/report"

  # Esperar a que el pipeline drene: correos en Mailpit == total (máx 20 min)
  for i in $(seq 1 80); do
    got=$(curl -s "$MP/api/v1/info" | python -c 'import json,sys; print(json.load(sys.stdin)["Messages"])')
    echo "  correos: $got / $total"
    [ "$got" -ge "$total" ] && break
    sleep 15
  done
  curl -s "$MP/api/v1/info" | python -c 'import json,sys; d=json.load(sys.stdin); print("  ", d["RuntimeStats"])'
done
```

**Criterio para pasar al siguiente escalón** (reporte en `target/jmeter/<total>/report/index.html` y
salida del script):

- Error rate < 1 % y ningún timeout.
- p95 que no haya crecido más del doble respecto al escalón anterior. Si se dispara, en el siguiente
  escalón no subas `threads`, solo `loops`.
- `SMTPAccepted` igual al total y `SMTPRejected` en 0. Si Mailpit se queda corto pasados 20 min, hay
  filas atascadas en el outbox: revisar los logs de `generator-service` y `notification-service` antes de
  seguir.

Si a 25 hilos el p95 ya se degrada, para 9000 usa `threads=25 loops=360` en vez de `30 × 300`. Si todo va
holgado, el techo razonable con estas réplicas es 40 hilos; más allá conviene subir `document-service` a
1 vCPU o a más réplicas, no más hilos.

#### Resultados obtenidos (9 de septiembre de 2026)

Corrida real sobre el escenario **document 3 · generator 6 · notification 6**, BD en `Standard_B4ms`,
correo hacia Mailpit. Los archivos están en [`docs/pruebas/`](docs/pruebas/): una captura del Summary
Report por escalón y el CSV crudo de JMeter con una fila por petición, listo para abrir en Excel (ver
[cómo exportar](#exportar-resultados-de-jmeter)). Los percentiles de la tabla se calcularon sobre esos CSV.

| Escalón | `threads` | Duración | Throughput | Promedio | Mediana | p90 | p95 | p99 | Máx | Errores | Correos en Mailpit |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 500 | 10 | 30 s | 16,9 req/s | 200 ms | 134 ms | 362 ms | 437 ms | 669 ms | 1.715 ms | 0 % | — |
| 9000 | 30 | 2 min 27 s | 61 req/s | 161 ms | 124 ms | 250 ms | 331 ms | 595 ms | 1.652 ms | 0 % | **9.049** |
| 20000 | 40 | 3 min 24 s | 97 req/s | 143 ms | 113 ms | 185 ms | 312 ms | 664 ms | 2.252 ms | 0 % | **22.278** |

| 500 | 9000 | 20000 |
|---|---|---|
| ![500](docs/pruebas/01-500-Summary-.png) | ![9000](docs/pruebas/02-9000-Summary-.png) | ![20000](docs/pruebas/03-20000-Summary-.png) |

**Lecturas:**

- **El API no es el cuello de botella.** Las 29.500 peticiones respondieron `200` sin un solo error ni
  timeout, y la latencia **bajó** al subir la concurrencia (p95 de 437 → 331 → 312 ms) porque los
  contenedores ya estaban calientes. Con 40 hilos el throughput llegó a 97 req/s, así que el techo de
  "15-20 peticiones en vuelo" estimado arriba es conservador para 3 réplicas: las duraciones reales fueron
  entre 6 y 8 veces menores que las estimadas en la tabla de la escalera.
- **El pipeline aguas abajo sí duplica.** Mailpit recibió **49 correos de más** en el escalón de 9000
  (0,5 %) y **2.278 de más** en el de 20000 (11,4 %). El exceso crece con la carga porque nace de la
  contención: el scheduler del outbox reenvía las filas `STARTED` cuyo ack de Kafka no llegó antes del
  siguiente tick, el scheduler de `generator-service` no usa `SKIP LOCKED` y cada réplica republica las
  mismas filas, el chequeo de idempotencia republica la respuesta en vez de solo saltarla, y el reintento
  SMTP tras un timeout de 5 s vuelve a entregar un correo que Mailpit ya había aceptado. Los índices
  únicos evitan filas duplicadas en BD, pero no efectos secundarios que ocurren antes del `INSERT`
  (generar el contenido, publicar a Kafka, enviar el correo). El detalle y el orden de corrección están en
  la [guía de Azure, sección de diagnóstico](docs/03-AZURE-ESTUDIANTE-PASO-A-PASO.md).
- **Corrección aplicada al envío de correo** (rama `feature/fix-duplicity`): notification-service ahora
  reserva la saga en `document_outbox` con estado `NOTIFICATION_PENDING` **antes** de enviar, usando el índice
  único como claim, y envía fuera de la transacción. Dos entregas del mismo `sagaId` ya no pueden producir
  dos correos: la segunda pierde el `INSERT` y se descarta. El detalle está en
  [Mecanismos contra la duplicidad](#mecanismos-contra-la-duplicidad). Siguen pendientes los reenvíos del
  scheduler del productor, el `SKIP LOCKED` del generator-service y el reintento SMTP tras timeout, que
  generan tráfico duplicado pero ya no correos duplicados.
- **Criterio de aceptación para la próxima corrida**: `SMTPAccepted == total`. Mientras Mailpit reciba
  más correos que peticiones, el sistema es "al menos una vez" en el envío y el conteo de la bandeja no
  sirve como medida de éxito. Con el claim-then-send ese debería ser el resultado; si no lo es, buscar
  primero filas `NOTIFICATION_PENDING` antiguas en `notification.document_outbox`.

> Las tres corridas se lanzaron desde la GUI de JMeter (se ve en las capturas). Para los números de latencia
> es válido porque el cliente no saturó, pero para 20000 o más conviene el modo CLI (`jmeter -n`) que se
> describe arriba: la GUI consume memoria por cada muestra y puede distorsionar el máximo.

#### Exportar resultados de JMeter

El archivo que JMeter escribe con `-l` (`.jtl`) **ya es CSV** aunque la extensión diga otra cosa; los de
`docs/pruebas/*-request.csv` son exactamente eso. Tres formas de sacar los datos a Excel u otro formato:

1. **Crudo, una fila por petición.** Abrir el `.jtl`/`.csv` desde Excel con *Datos → Desde texto/CSV*. El
   `timeStamp` es epoch en milisegundos; en Excel: `=A2/86400000 + DATE(1970,1,1)` con formato de fecha.
   Para que salga listo desde el inicio (extensión `.csv`, punto y coma, fecha legible):

   ```bash
   jmeter -n -t docs/jmeter/create-document.jmx \
     -Jjmeter.save.saveservice.output_format=csv \
     -Jjmeter.save.saveservice.default_delimiter=";" \
     -Jjmeter.save.saveservice.timestamp_format="yyyy-MM-dd HH:mm:ss" \
     -l target/jmeter/results.csv -e -o target/jmeter/report
   ```

2. **Tabla resumen (percentiles, throughput, errores).** En la GUI, cargar el `.jtl` en un *Aggregate Report*
   (botón *Browse*) y pulsar **Save Table Data**. Sin GUI, con el plugin *Command-Line Graph Plotting Tool*:

   ```bash
   JMeterPluginsCMD --generate-csv target/jmeter/aggregate.csv \
     --input-jtl target/jmeter/results.jtl --plugin-type AggregateReport
   ```

3. **Desde el reporte HTML que ya generas.** `target/jmeter/report/statistics.json` trae la misma tabla del
   dashboard. A CSV en una línea de PowerShell, útil para consolidar un archivo por escalón:

   ```powershell
   $s = Get-Content target/jmeter/report/statistics.json | ConvertFrom-Json
   $s.PSObject.Properties.Value |
     Select-Object transaction, sampleCount, errorCount, errorPct, meanResTime, pct1ResTime, pct2ResTime, pct3ResTime, throughput |
     Export-Csv target/jmeter/statistics.csv -NoTypeInformation
   ```

JMeter no genera `.xlsx` directo: la ruta es CSV → Excel, o `pandas.read_csv(...).to_excel(...)`. Para ver
métricas en vivo, agregar un *Backend Listener* hacia InfluxDB/Grafana. Evitar `output_format=xml` en
corridas grandes: solo sirve si se necesita request/response completos y multiplica el tamaño del archivo.

#### Apagar todo y volver a la BD barata

⚠️ **Ejecútalo el mismo día de la prueba.** Fuera del `Standard_B1ms` se cobra ~$115/mes; el
`Standard_D2s_v3` cuesta ~$0.16/hora mientras siga encendido.

```bash
# 1. Servicios al modo ahorro (scale-to-zero: dejan de cobrar cuando nadie los llama)
for s in document-service customer-service generator-service notification-service; do
  az containerapp update -g dns-student-rg -n $s --min-replicas 0 --max-replicas 1
done

# 2. BD de vuelta al tamaño gratuito
az postgres flexible-server update -g dns-student-rg -n dns-student-pg \
  --sku-name Standard_B1ms --tier Burstable

# 3. Pausar la BD hasta la próxima sesión
az postgres flexible-server stop -g dns-student-rg -n dns-student-pg
```

Para verificar que no quedó nada encendido:

```bash
az containerapp list -g dns-student-rg \
  --query "[].{name:name, min:properties.template.scale.minReplicas, max:properties.template.scale.maxReplicas}" -o table
az postgres flexible-server show -g dns-student-rg -n dns-student-pg \
  --query "{sku:sku.name, tier:sku.tier, state:state}" -o table
```

El detalle de escalado (requisitos previos, límites de la BD, Mailpit para pruebas de carga y costos por sesión) está en la [sección de escalado de la guía](docs/03-AZURE-ESTUDIANTE-PASO-A-PASO.md#escalado-múltiples-instancias-manual-y-automático).

## Tecnologías y patterns recomendados

### Stack tecnológico actual

| Categoría | Tecnología | Uso |
|-----------|------------|-----|
| **Lenguaje** | Java 19 | Desarrollo de servicios |
| **Framework** | Spring Boot 3.x | Contenedor de aplicación |
| **Build** | Maven | Gestión de dependencias y build |
| **Persistencia** | PostgreSQL | Base de datos relacional |
| **Mensajería** | Apache Kafka | Comunicación asíncrona entre servicios |
| **Mailing** | Azure Communication Services (default) / Java Mail SMTP | Envío de notificaciones por email, seleccionable con `MAIL_PROVIDER` |
| **Contenerización** | Docker | Despliegue de infraestructura |
| **Testing** | JUnit 5, Mockito, TestContainers | Pruebas unitarias e integración |

### Patterns aplicados

| Pattern | Descripción | Implementación |
|---------|-------------|----------------|
| **Hexagonal Architecture** | Separación entre lógica de negocio e infraestructura | Ports & Adapters en cada servicio |
| **Domain-Driven Design** | Modelado del negocio centrado en el dominio | Bounded contexts, entidades, agregados |
| **CQRS** | Separación de comandos y consultas | Diferentes modelos para lectura/escritura |
| **Event-Driven** | Comunicación mediante eventos | Kafka producers/consumers |
| **Outbox Pattern** | Garantía de entrega de eventos | Tabla outbox para eventos pendientes |
| **Saga Pattern** | Gestión de transacciones distribuidas | Orquestación de procesos entre servicios |

### Infraestructura recomendada para producción

- **Brokers/Streams**: Apache Kafka, RabbitMQ, Redis Streams o AWS SNS/SQS
- **Bases de datos**: PostgreSQL (fuente de verdad) + Redis/Elasticsearch (lecturas rápidas)
- **Observabilidad**: OpenTelemetry (tracing), Prometheus (métricas), Grafana (visualización), ELK (logs)
- **Orquestación**: Kubernetes con Helm charts
- **CI/CD**: GitHub Actions, Jenkins o GitLab CI

## Cómo empezar (resumen)

### Prerrequisitos
- Java 19 (el `maven-compiler-plugin` usa `<release>19</release>`; con 17 el build falla)
- Maven 3.8+
- Docker y Docker Compose (para infraestructura)
- Git

### Instalación y ejecución

1. Clona el repositorio:
```bash
git clone https://github.com/Rincon10/DOCUMENT-NOTIFICATION-SYSTEM.git
cd DOCUMENT-NOTIFICATION-SYSTEM
```

2. Inicia la infraestructura (bases de datos, Kafka, etc.):
```bash
cd document-notification-system
 docker-compose up -d
```

3. Compila el proyecto:
```bash
mvn clean install
```

4. Ejecuta los servicios individualmente:
```bash
# Document Service
mvn -pl document-service/document-container spring-boot:run

# Generator Service
mvn -pl generator-service/generator-container spring-boot:run

# Notification Service
mvn -pl notification-service/notification-container spring-boot:run
```

### Verificación de la arquitectura

Para validar que las dependencias siguen las reglas de DDD, genera los grafos de dependencias:

```bash
# Grafo individual de cada módulo
mvn com.github.ferstl:depgraph-maven-plugin:graph

# Grafo agregado completo
mvn com.github.ferstl:depgraph-maven-plugin:aggregate -DcreateImage=true -DreduceEdges=false -Dscope=compile "-Dincludes=com.document.notification.system*:*"
```

Los grafos generados se encuentran en `target/dependency-graph.png` de cada módulo.
## Buenas prácticas y recomendaciones

### Desarrollo

- **Mantén la lógica de negocio en el dominio**: Los casos de uso orquestan, pero las reglas de negocio viven en `domain-core`. Evita lógica de negocio en controladores o adaptadores.
- **Dependencias unidireccionales**: Las capas externas dependen de las internas, nunca al revés. El dominio no conoce frameworks ni infraestructura.
- **Inmutabilidad**: Prefiere value objects inmutables para garantizar consistencia y thread-safety.
- **Lenguaje ubicuo**: Usa el mismo vocabulario del negocio en código, eventos y documentación.

### Testing

- **Pirámide de tests**: Mayoría de tests unitarios en el dominio, menos tests de integración, pocos tests E2E.
- **Tests de contratos**: Verifica que los eventos publicados cumplen el esquema esperado por los consumidores.
- **TestContainers**: Usa contenedores reales para tests de integración con bases de datos y brokers.

### Arquitectura distribuida

- **Idempotencia**: Implementa idempotencia en consumidores de eventos y endpoints públicos para manejar reintentos.
- **Eventos de dominio versionados**: Gestiona versiones de eventos y migraciones de esquema con cuidado.
- **Contratos claros**: Documenta los eventos de dominio que cruzan bounded contexts (payload, version, semántica).
- **Circuit Breaker**: Implementa circuit breakers para llamadas a servicios externos.
- **Dead Letter Queues**: Configura DLQ para mensajes que fallan procesamiento repetidamente.

### Seguridad

- **Validación en boundaries**: Valida todas las entradas en los adaptadores de entrada (controllers, listeners).
- **Sanitización**: Limpia datos antes de persistir o renderizar.
- **Autenticación/Autorización**: Implementa JWT/OAuth2 en los adaptadores de entrada.

## Contribuir

¡Las contribuciones son bienvenidas! Este proyecto busca ser una referencia de arquitectura limpia en Java.

### Proceso de contribución

1. **Lee la documentación**: Revisa los ADRs y diagramas en `docs/` para entender las decisiones de diseño.
2. **Discute antes de implementar**: Abre un issue para discusiones de diseño antes de cambios grandes.
3. **Fork y branch**: Crea un fork y trabaja en una feature branch (`git checkout -b feature/nueva-funcionalidad`).
4. **Commits convencionales**: Usa [Conventional Commits](https://www.conventionalcommits.org/):
   - `feat:` nueva funcionalidad
   - `fix:` corrección de bug
   - `docs:` cambios en documentación
   - `refactor:` refactorización de código
   - `test:` cambios en tests
5. **Pull Request**: Las PRs deben incluir:
   - Tests relevantes (unitarios y/o de integración)
   - Documentación actualizada si cambian APIs o contratos
   - Descripción clara del cambio y motivación
   - Verificación de que no viola las reglas de arquitectura (`mvn com.github.ferstl:depgraph-maven-plugin:graph`)

### Verificación de calidad

Antes de enviar tu PR, asegúrate de:

```bash
# Compilar sin errores
mvn clean compile

# Tests pasando
mvn test

# Verificar reglas de arquitectura (no debe haber dependencias de dominio hacia infraestructura)
mvn com.github.ferstl:depgraph-maven-plugin:graph
```

### Código de conducta

- Sé respetuoso y constructivo en las discusiones
- Prioriza la claridad sobre la complejidad
- Documenta las decisiones arquitectónicas importantes
- Ayuda a mantener la calidad del código

## Autores

- **Rincon10** - *Trabajo inicial* - [GitHub](https://github.com/Rincon10)

## Licencia

Este proyecto está licenciado bajo la Licencia MIT.

---

<p align="center">
  <i>Construido con ❤️ siguiendo principios de Domain-Driven Design y Clean Architecture</i>
</p>
