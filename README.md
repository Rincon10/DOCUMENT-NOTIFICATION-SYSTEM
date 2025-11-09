# DOCUMENT-NOTIFICATION-SYSTEM

Un sistema de notificaciones distribuido orientado a documentos, diseñado con principios de arquitecturas limpias (Clean Architecture) y prácticas de diseño como Hexagonal Architecture y Domain-Driven Design (DDD). El objetivo es servir como base sólida y extensible para ejecutar notificaciones en entornos distribuidos con buena separación de responsabilidades, alta testabilidad y capacidad de evolución.

## Visión general
Este repositorio contiene el código fuente y la estructura para un sistema que produce, enruta y entrega notificaciones relacionadas con documentos (por ejemplo: creación, actualización, expiración, aprobaciones). Está pensado para ser desplegado de forma distribuida, integrándose con brokers de mensajería, colas y/o eventos y exponiendo adaptadores (API, webhook, colas) según las necesidades.

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

## Estructura típica del repositorio
(La estructura concreta puede diferir según la implementación; aquí se muestran capas conceptuales)

- domain/            -> Entidades, agregados, value objects, eventos de dominio
- application/       -> Casos de uso, orquestadores de flujos de negocio
- adapters/          -> Adaptadores entrantes (HTTP, gRPC) y salientes (DB, Broker)
- infrastructure/    -> 
- config/            -> 
- tests/             -> Pruebas unitarias, de integración y contract tests
- docs/              -> Diagramas, decisiones arquitectónicas (ADR)

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
   git clone https://github.com/Rincon10/DOCUMENT-NOTIFICATION-SYSTEM.git

2.
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
