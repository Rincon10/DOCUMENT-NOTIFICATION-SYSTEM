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
│   └── jmeter/create-document.jmx           # Plan de JMeter para POST /documents
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
[`docs/BATCHTEST.md`](docs/BATCHTEST.md).

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
