# DOCUMENT-NOTIFICATION-SYSTEM

Un sistema de notificaciones distribuido orientado a documentos, diseñado con principios de arquitecturas limpias (Clean Architecture) y prácticas de diseño como Hexagonal Architecture y Domain-Driven Design (DDD). El objetivo es servir como base sólida y extensible para ejecutar notificaciones en entornos distribuidos con buena separación de responsabilidades, alta testabilidad y capacidad de evolución.

## Visión general
Este repositorio contiene el código fuente y la estructura para un sistema que produce, enruta y entrega notificaciones relacionadas con documentos (por ejemplo: creación, actualización, expiración, aprobaciones). Está pensado para ser desplegado de forma distribuida, integrándose con brokers de mensajería, colas y/o eventos y exponiendo adaptadores (API, webhook, colas) según las necesidades.

### Arquitectura basada en Domain-Driven Design
El siguiente diagrama ilustra la aplicación de los principios de Domain-Driven Design (DDD) en el sistema, mostrando cómo se organizan las capas, bounded contexts, entidades, agregados y value objects. Esta estructura garantiza que el dominio permanezca en el centro de la arquitectura, con las dependencias apuntando hacia el núcleo del negocio.


![Arquitectura DDD](docs/00-arquitectura-DDD.png)

## Estructura típica del repositorio
(La estructura concreta puede diferir según la implementación; aquí se muestran capas conceptuales)

- domain/            -> Entidades, agregados, value objects, eventos de dominio
- application/       -> Casos de uso, orquestadores de flujos de negocio
- adapters/          -> Adaptadores entrantes (HTTP, gRPC) y salientes (DB, Broker)
- infrastructure/    -> 
- config/            -> 
- tests/             -> Pruebas unitarias, de integración y contract tests
- docs/              -> Diagramas, decisiones arquitectónicas (ADR)

## Arquitectura general del sistema
El siguiente diagrama muestra el flujo general de la arquitectura del sistema, ilustrando cómo los diferentes componentes interactúan entre sí en un entorno distribuido. Se puede observar la separación de responsabilidades, la comunicación entre servicios, y cómo fluyen los datos desde la entrada hasta la entrega de notificaciones.

![Flujo general de la arquitectura](docs/00-flujo-generarl-arquitectura.png)

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

![Arquitectura del componente Document Service](docs/01-arquitectura-componente-document.png)

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

## Tecnologías y patterns recomendados
- Brokers/Streams: Kafka, RabbitMQ, Redis Streams o AWS SNS/SQS (según el entorno).
- Bases de datos: PostgreSQL (como fuente de verdad) + almacenes de consulta (Redis, Elastic) para lecturas rápidas.
- Observabilidad: Tracing (OpenTelemetry), métricas (Prometheus), logs estructurados.
- Pruebas: Unitarias en el dominio y tests de contratos entre adaptadores e infra.
- Contenerización: Docker + orquestación (Kubernetes) para despliegue distribuido.

## Cómo empezar (resumen)
1. Clona el repositorio:
```
   git clone https://github.com/Rincon10/DOCUMENT-NOTIFICATION-SYSTEM.git
```
3.
## Buenas prácticas y recomendaciones
- Mantiene la lógica de negocio en el dominio y los casos de uso; evita lógica de negocio en controladores o adaptadores.
- Prefiere pruebas unitarias en el dominio y pruebas de integración atómicas para adaptadores.
- Define contratos claros entre bounded contexts vía eventos de dominio documentados.
- Implementa idempotencia en consumidores de eventos y en endpoints expuestos públicamente.
- Gestiona versiones de eventos y migraciones del esquema de mensajes con cuidado.

## Contribuir
Si quieres contribuir:
- Lee los ADRs y la documentación en docs/ para entender las decisiones de diseño.
- Abre issues para discusiones de diseño antes de cambios grandes.
- Las PRs deben incluir tests relevantes y actualizar la documentación si cambian APIs o contratos.
