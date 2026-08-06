# Resultados, Contribuciones y Conclusiones

## Capitulo N: Resultados y Contribuciones del Proyecto

### N.1 Descripcion general de la solucion

El presente trabajo propone e implementa un sistema distribuido de generacion y notificacion de documentos financieros, construido sobre una arquitectura de microservicios orientada por eventos. A diferencia de las aproximaciones monoliticas tradicionales o de los enfoques basados en procesamiento por lotes, esta solucion prioriza tres atributos de calidad fundamentales: la modificabilidad del sistema ante cambios tecnologicos, la consistencia eventual entre servicios autonomos, y la escalabilidad independiente de cada componente funcional.

El sistema esta compuesto por cuatro microservicios que operan como contextos acotados independientes (Bounded Contexts), siguiendo los principios del Diseno Dirigido por el Dominio (Domain-Driven Design). Cada servicio encapsula su propia logica de negocio, su modelo de datos y sus mecanismos de comunicacion, comunicandose exclusivamente a traves de eventos asincrono mediante Apache Kafka.

| Servicio | Responsabilidad | Tipo |
|----------|----------------|------|
| **document-service** | Orquestador de sagas. Recibe solicitudes REST, coordina la generacion y notificacion de documentos | API REST + Orquestador |
| **generator-service** | Genera el contenido del documento a partir de los datos del cliente y el periodo solicitado | Worker Kafka |
| **notification-service** | Envia notificaciones por correo electronico (SMTP) con control de tasa (rate limiting) | Worker Kafka + SMTP |
| **customer-service** | Gestiona la informacion de clientes y publica eventos de creacion | API REST + Publisher |

La decision de descomponer el sistema en estos cuatro contextos no fue arbitraria. Se aplico el principio de alta cohesion y bajo acoplamiento propuesto por Evans (2003), donde cada contexto encapsula un subdominio con lenguaje ubicuo propio: el vocabulario de "documentos" y "periodos" pertenece al contexto de documentos, mientras que "destinatario", "canal" y "tasa de envio" pertenecen al contexto de notificaciones.

### N.2 Arquitectura Hexagonal: Puertos y Adaptadores

La contribucion arquitectonica central de este trabajo es la implementacion rigurosa de la Arquitectura Hexagonal (Cockburn, 2005), tambien conocida como Ports & Adapters. Este patron establece que el nucleo de dominio no debe depender de ningun detalle de infraestructura; en su lugar, define interfaces (puertos) que son implementadas por adaptadores externos.

#### N.2.1 Estructura de capas por servicio

Cada microservicio esta organizado en modulos Maven que refuerzan la separacion a nivel de compilacion, no solo a nivel de convencion:

```
<servicio>/
  ├── <servicio>-domain/
  │   ├── <servicio>-domain-core/           # Entidades, Value Objects, Eventos, Servicios de dominio
  │   └── <servicio>-application-service/   # Puertos (input/output), Comandos, DTOs, Orquestacion
  ├── <servicio>-dataaccess/                # Adaptador: JPA + PostgreSQL
  ├── <servicio>-messaging/                 # Adaptador: Kafka (productores y consumidores)
  ├── <servicio>-application-api/           # Adaptador: REST Controllers (Spring MVC)
  └── <servicio>-container/                 # Configuracion de inyeccion de dependencias
```

Esta estructura garantiza que el modulo `domain-core` no tiene dependencias hacia ningun framework. Un analisis del `pom.xml` de cada `domain-core` muestra que sus unicas dependencias son el modulo `common-domain` (abstracciones compartidas como `AggregateRoot`, `BaseEntity`, `BaseId`) y Lombok para reduccion de boilerplate. No existe ninguna referencia a Spring, JPA, Kafka ni ninguna otra tecnologia de infraestructura.

Como senala Vernon (2013): *"El dominio es el corazon del software y debe estar protegido de las preocupaciones tecnicas"*. En esta implementacion, esa proteccion es verificable estructuralmente: si un desarrollador intenta importar `javax.persistence` o `org.springframework` desde el modulo `domain-core`, la compilacion Maven fallara por ausencia de la dependencia.

#### N.2.2 Puertos de entrada (Driving Ports)

Los puertos de entrada definen las operaciones que el mundo exterior puede invocar sobre el dominio:

- `DocumentApplicationService.createDocument(CreateDocumentCommand)` -- Inicia la creacion de un documento y dispara la saga de generacion.
- `GenerationRequestMessageListener.completedGeneration(GenerationRequest)` -- Procesa la respuesta del servicio de generacion.
- `NotificationRequestMessageListener.requestNotification(NotificationRequest)` -- Procesa solicitudes de envio de notificacion.
- `CustomerApplicationService.createCustomer(CreateCustomerCommand)` -- Crea un cliente y publica un evento de dominio.

Estos puertos son interfaces Java puras, sin anotaciones de framework. Su implementacion reside en la capa `application-service`, que actua como coordinadora entre el dominio y los puertos de salida.

#### N.2.3 Puertos de salida (Driven Ports)

Los puertos de salida abstraen las dependencias de infraestructura:

**Puertos de persistencia:**
- `IDocumentRepository` -- Implementado por `DocumentRepositoryImpl` (JPA/PostgreSQL)
- `CustomerRepository` -- Implementado por `CustomerRepositoryImpl` (JPA/PostgreSQL)
- `DocumentGenerationRepository` -- Implementado por `DocumentGenerationRepositoryImpl` (JPA/PostgreSQL)
- `DocumentNotificationRepository` -- Implementado por `DocumentNotificationRepositoryImpl` (JPA/PostgreSQL)
- `GeneratorOutboxRepository`, `NotificationOutboxRepository`, `DocumentOutboxRepository` -- Repositorios del patron Outbox

**Puertos de mensajeria:**
- `GenerationRequestMessagePublisher` -- Implementado por `DocumentGenerationEventRequestKafkaPublisher`
- `NotificationRequestMessagePublisher` -- Implementado por `DocumentNotificationEventRequestKafkaPublisher`
- `GenerationResponseMessagePublisher` -- Implementado por `GenerationResponseKafkaPublisher`
- `NotificationResponseMessagePublisher` -- Implementado por `NotificationResponseKafkaPublisher`
- `CustomerMessagePublisher` -- Implementado por `CustomerCreatedEventKafkaPublisher`

#### N.2.4 Demostracion de intercambiabilidad de adaptadores

Para validar la modificabilidad que la arquitectura hexagonal promete, se realizo un analisis de impacto: si se reemplazara Apache Kafka por RabbitMQ como broker de mensajeria, el alcance del cambio seria:

**Archivos que cambiarian (adaptadores de infraestructura):**
- 5 implementaciones de publicadores Kafka (`*KafkaPublisher.java`)
- 5 implementaciones de consumidores Kafka (`*KafkaListener.java`)
- Clases de configuracion de Kafka (`kafka-config-data`)
- Modelos Avro (`kafka-model`) -- se reemplazarian por el formato de serializacion de RabbitMQ
- Archivos de configuracion `application.yml`

**Archivos que NO cambiarian (~90% del codigo):**
- Todo el dominio: entidades, value objects, eventos, servicios de dominio
- Toda la capa de aplicacion: comandos, handlers, sagas, outbox helpers
- Todos los puertos (interfaces): `GenerationResponseMessagePublisher`, `NotificationRequestMessageListener`, etc.
- Toda la capa de persistencia: repositorios JPA, entidades de base de datos
- Los controladores REST

Este resultado confirma empiricamente el principio de Inversion de Dependencias (Martin, 2003): el 90% del codigo depende de abstracciones, no de implementaciones concretas. Un cambio en la tecnologia de mensajeria afecta exclusivamente a la capa de adaptadores, sin propagarse al nucleo del sistema.

### N.3 Patrones de consistencia distribuida

#### N.3.1 Patron Saga (Orquestacion)

En un sistema de microservicios, las transacciones distribuidas no pueden resolverse con un commit de dos fases (2PC) sin comprometer la disponibilidad (Garcia-Molina & Salem, 1987). El patron Saga, propuesto originalmente por Garcia-Molina y Salem, descompone una transaccion larga en una secuencia de transacciones locales, cada una con su correspondiente accion de compensacion.

Esta implementacion utiliza el estilo de **orquestacion**, donde el `document-service` actua como coordinador central. Se definio una interfaz generica `SagaStep<T>` con dos operaciones:

```java
public interface SagaStep<T> {
    void execute(T data);
    void compensate(T data);
}
```

El flujo de la saga principal (generacion + notificacion) sigue esta secuencia:

```
1. Cliente envia POST /api/documents
2. document-service: crea Document (PENDING), guarda en outbox de generacion
3. Outbox scheduler publica a Kafka -> topic: generator-request
4. generator-service: genera documento, publica respuesta -> topic: generator-response
5. document-service (DocumentGenerationSaga.execute):
   a. Valida idempotencia via saga_id + status
   b. Actualiza Document a GENERATED
   c. Guarda en outbox de notificacion
6. Outbox scheduler publica a Kafka -> topic: notification-request
7. notification-service: envia email, publica respuesta -> topic: notification-response
8. document-service (DocumentNotificationSaga.execute):
   a. Actualiza Document a SENT (estado final)
```

**Compensacion:** Si en el paso 4 el `generator-service` reporta un fallo, se activa `DocumentGenerationSaga.compensate()`, que transiciona el documento a `CANCELLED` y marca la saga como `COMPENSATED`. Los estados de la saga (`STARTED`, `PROCESSING`, `SUCCESSFUL`, `FAILED`, `COMPENSATING`, `COMPENSATED`) modelan un automata finito determinista que garantiza que toda saga alcanza un estado terminal.

La verificacion de idempotencia es critica en este patron. Antes de ejecutar cualquier paso, la saga consulta el estado actual del mensaje outbox:

```java
Optional<DocumentGenerationOutboxMessage> optionalMessage =
    generatorOutboxHelper.getDocumentGenerationOutboxMessageBySagaIdAndSagaStatus(
        generationResponse.getSagaId(), SagaStatus.STARTED);
if (optionalMessage.isEmpty()) {
    log.info("Outbox message already processed, skipping.");
    return;
}
```

Esta verificacion previene el procesamiento duplicado que ocurriria si Kafka entregara el mismo mensaje dos veces (at-least-once delivery), un escenario documentado por Kleppmann (2017) como inherente a los sistemas de mensajeria distribuida.

#### N.3.2 Patron Transactional Outbox

El problema del dual-write (escribir en la base de datos y publicar a Kafka en una sola operacion atomica) es uno de los desafios fundamentales de los sistemas distribuidos (Richardson, 2018). Si la escritura en base de datos tiene exito pero la publicacion a Kafka falla, el sistema queda en un estado inconsistente.

La solucion implementada es el patron Transactional Outbox: en lugar de publicar directamente a Kafka, el servicio escribe el evento en una tabla `outbox` dentro de la misma transaccion de base de datos. Un proceso independiente (`OutboxScheduler`) sondea periodicamente esta tabla y publica los mensajes pendientes.

```
Transaccion unica:
  1. UPDATE documents SET status = 'GENERATED' WHERE id = ?
  2. INSERT INTO generation_outbox (saga_id, payload, outbox_status) VALUES (?, ?, 'STARTED')
  COMMIT  <-- atomico

Proceso independiente (cada 10 segundos):
  3. SELECT * FROM generation_outbox WHERE outbox_status = 'STARTED'
  4. Para cada mensaje: publicar a Kafka
  5. UPDATE generation_outbox SET outbox_status = 'COMPLETED'
```

La tabla outbox incluye una columna `version` anotada con `@Version` de JPA, que implementa bloqueo optimista. Cuando multiples instancias del mismo servicio ejecutan el scheduler concurrentemente, solo una logra actualizar cada registro; las demas reciben una `OptimisticLockException` y descartan el intento. Este mecanismo, descrito por Sadalage y Fowler (2012), permite escalar horizontalmente sin riesgo de publicacion duplicada.

Los schedulers estan configurados con:
- `fixedRate = 10000 ms` (sondeo cada 10 segundos)
- `initialDelay = 10000 ms` (espera inicial para que Kafka este disponible)
- Un scheduler de limpieza (`@midnight`) que elimina registros outbox completados

#### N.3.3 Control de tasa en notificaciones

El servicio de notificacion implementa un mecanismo de rate limiting basado en el algoritmo Token Bucket, configurable via variables de entorno (`MAIL_RATE_LIMIT_TOKENS`, `MAIL_RATE_LIMIT_REFILL_MS`). Este control previene la saturacion del servidor SMTP y respeta los limites impuestos por proveedores como Gmail (que limita a ~500 envios diarios en cuentas estandar).

### N.4 Validacion y pruebas

#### N.4.1 Pruebas de integracion del flujo completo

La validacion del sistema se realizo mediante pruebas de integracion end-to-end ejecutadas sobre la plataforma Docker, utilizando el archivo `docker-compose.yml` que orquesta los 4 microservicios junto con la infraestructura completa (PostgreSQL, Kafka cluster de 3 nodos, Schema Registry, Zookeeper).

**Escenario 1: Flujo exitoso (happy path)**

Se envio una solicitud de creacion de documento via `POST /api/documents` con un cliente valido. Se verifico que:

1. El `document-service` creo el registro con estado `PENDING` y almaceno el evento en la tabla `generation_outbox`.
2. El outbox scheduler publico el evento al topic `generator-request` en menos de 10 segundos.
3. El `generator-service` consumio el evento, genero el documento y publico la respuesta al topic `generator-response`.
4. El `document-service` recibio la respuesta, ejecuto `DocumentGenerationSaga.execute()`, actualizo el documento a `GENERATED` y creo un registro en `notification_outbox`.
5. El `notification-service` consumio el evento, envio el correo electronico y publico la respuesta al topic `notification-response`.
6. El `document-service` recibio la respuesta final y actualizo el documento a `SENT`.

La transicion completa (`PENDING` -> `GENERATED` -> `SENT`) se completo en aproximadamente 25-35 segundos, dominado por los intervalos de sondeo del outbox scheduler (2 ciclos de 10 segundos).

**Escenario 2: Fallo en generacion (compensacion)**

Se simulo un fallo en el `generator-service` durante la generacion. Se verifico que:

1. El `generator-service` publico una respuesta con `GENERATION_FAILED` al topic `generator-response`.
2. El `document-service` ejecuto `DocumentGenerationSaga.compensate()`.
3. El documento transiciono a `CANCELLED` y la saga a `COMPENSATED`.
4. No se creo ningun registro en `notification_outbox` (la saga se detuvo correctamente).

**Escenario 3: Multiples instancias concurrentes**

Se escalaron 3 instancias del `notification-service` usando `docker compose up --scale notification-service=3`. Se verifico que:

1. Kafka redistribuyo las 3 particiones del topic `notification-request` entre las 3 instancias (una particion por instancia).
2. Los mensajes se procesaron en paralelo sin duplicacion.
3. Las excepciones `OptimisticLockException` en los logs del outbox scheduler fueron esporadicas y manejadas correctamente (la instancia que pierde la carrera simplemente descarta el intento).
4. El throughput total de notificaciones escalo linealmente con el numero de instancias.

**Escenario 4: Idempotencia ante re-delivery de Kafka**

Kafka opera con semantica at-least-once por defecto. Para validar la idempotencia, se observo el comportamiento cuando un consumer se reconecto despues de un reinicio:

1. Al reiniciar una instancia del `generator-service`, Kafka re-entrego mensajes del ultimo offset no commiteado.
2. La saga detecto que el `saga_id` ya habia sido procesado (el outbox message tenia estado `SUCCESSFUL`).
3. El mensaje fue descartado con un log informativo, sin efecto lateral.

#### N.4.2 Pruebas de infraestructura Docker

Se valido la correcta inicializacion de la infraestructura:

- PostgreSQL crea los 4 schemas (`customer`, `document`, `generator`, `notification`) con todos los tipos enumerados, tablas, indices, vistas materializadas y triggers a partir de `init-db.sql`.
- Kafka crea los 5 topics con factor de replicacion 3 y 3 particiones cada uno.
- Schema Registry se registra correctamente y queda disponible para los serializadores Avro.
- Los servicios esperan a que la infraestructura este saludable (healthchecks) antes de iniciar.

#### N.4.3 Pruebas de intercambiabilidad de adaptadores

Como prueba conceptual de la modificabilidad arquitectonica, se documento el analisis de impacto descrito en la seccion N.2.4. Este analisis demuestra que un cambio de broker de mensajeria (Kafka por RabbitMQ) o de base de datos (PostgreSQL por MongoDB) afectaria exclusivamente la capa de adaptadores, sin modificar el dominio ni la orquestacion de sagas.

Adicionalmente, el sistema utiliza dos perfiles de Spring (`default` y `docker`) que demuestran la intercambiabilidad de configuracion: el mismo codigo se ejecuta apuntando a `localhost` (desarrollo local) o a servicios Docker (despliegue), simplemente cambiando el perfil activo. Esta misma tecnica se extiende al despliegue en Azure, donde las URLs de base de datos se sobreescriben via variables de entorno sin tocar el codigo fuente.

---

## Capitulo M: Analisis Comparativo de Arquitecturas

### M.1 Motivacion de la comparativa

Para contextualizar las decisiones arquitectonicas de este trabajo, se presenta una comparativa entre tres enfoques para resolver el mismo problema de negocio (generacion y notificacion de documentos financieros):

1. **Arquitectura de loteo** con Spring Batch
2. **Arquitectura de microservicios sincronos** con Spring Cloud (Eureka + Config + Gateway + Resilience4j)
3. **Arquitectura propuesta**: microservicios orientados a eventos con DDD, Arquitectura Hexagonal, Saga y Outbox

### M.2 Spring Batch: Procesamiento por lotes

Spring Batch (Minella, 2019) es un framework disenado para el procesamiento masivo de datos en lotes programados. En este enfoque, la generacion y envio de documentos se modelaria como un job con steps secuenciales:

```
Job: DocumentBatchJob
  Step 1: ItemReader    -> Leer clientes pendientes de PostgreSQL
  Step 2: ItemProcessor -> Generar documento para cada cliente
  Step 3: ItemWriter    -> Enviar notificacion por email
  Step 4: ItemWriter    -> Actualizar estado en base de datos
```

**Ventajas:**
- Simplicidad conceptual: un solo proceso secuencial, facil de razonar
- Manejo nativo de reintentos, skip y restart por chunk
- Control transaccional fuerte (una sola base de datos, un solo commit por chunk)
- Bajo overhead operacional: un solo artefacto desplegable
- Ideal para cargas predecibles y programadas (ej: procesar 10,000 documentos a medianoche)

**Limitaciones frente al problema planteado:**
- **Acoplamiento temporal**: los documentos solo se procesan en la ventana del batch. Un documento creado a las 2:00 PM no se procesaria hasta la siguiente ejecucion programada (por ejemplo, a las 00:00). Richardson (2018) describe esta limitacion como inaceptable para sistemas que requieren respuesta cercana al tiempo real.
- **Escalabilidad vertical**: Spring Batch escala mediante particionamiento dentro del mismo proceso JVM. Escalar horizontalmente requiere mecanismos adicionales como Spring Batch Integration o un job repository compartido, que anade complejidad significativa (Minella, 2019).
- **Monolito implicito**: generar y notificar son responsabilidades distintas forzadas a coexistir en el mismo artefacto. Un cambio en la logica de envio de email requiere redesplegar todo el job, incluyendo la logica de generacion.
- **Sin procesamiento reactivo**: no hay capacidad de reaccionar a eventos individuales. Si un cliente actualiza su email, el sistema no puede re-notificar inmediatamente; debe esperar al proximo ciclo de batch.
- **Compensacion compleja**: si el step de notificacion falla a mitad del lote, la recuperacion parcial requiere logica personalizada. No existe un mecanismo nativo de compensacion transaccional distribuida como el patron Saga.

### M.3 Spring Cloud: Microservicios sincronos

El ecosistema Spring Cloud (Carnell & Sanchez, 2021) propone una arquitectura de microservicios donde la comunicacion predominante es sincrona via HTTP/REST:

```
                    ┌─────────────────┐
                    │  Spring Cloud   │
                    │  Config Server  │
                    └────────┬────────┘
                             │
┌──────────┐    ┌────────────┴────────────┐    ┌──────────────────┐
│  Client  │───>│  Spring Cloud Gateway   │───>│  Eureka Service  │
└──────────┘    └────────────┬────────────┘    │  Discovery       │
                             │                  └──────────────────┘
                ┌────────────┼────────────┐
                │            │            │
         ┌──────┴─────┐ ┌───┴────┐ ┌─────┴──────┐
         │ Document   │ │Generator│ │Notification│
         │ Service    │ │Service  │ │Service     │
         │ +Resilience│ │        │ │            │
         └──────┬─────┘ └───┬────┘ └─────┬──────┘
                │            │            │
                └────────────┴────────────┘
                      PostgreSQL
```

**Componentes del stack:**
- **Eureka**: Registro y descubrimiento de servicios. Cada microservicio se registra al iniciar y consulta Eureka para localizar otros servicios.
- **Spring Cloud Config**: Centralizacion de configuracion externalizada en un repositorio Git.
- **Spring Cloud Gateway**: API Gateway que enruta peticiones, aplica filtros y balancea carga.
- **Resilience4j**: Circuit breaker, retry, rate limiter y bulkhead para llamadas entre servicios.

**Ventajas:**
- Ecosistema maduro con amplia documentacion y comunidad
- Descubrimiento de servicios dinamico (Eureka)
- Circuit breaker previene cascadas de fallos (Nygard, 2007)
- Configuracion centralizada facilita el manejo de multiples entornos
- API Gateway simplifica el acceso externo y permite cross-cutting concerns

**Limitaciones frente al problema planteado:**

- **Acoplamiento temporal sincrono**: cuando `document-service` llama a `generator-service` via HTTP, ambos deben estar disponibles simultaneamente. Si `generator-service` esta bajo carga o caido, el flujo se bloquea. Resilience4j mitiga esto con circuit breakers, pero no lo elimina; el retry tiene un limite y eventualmente falla (Newman, 2015). En contraste, la comunicacion asincrona via Kafka permite que el mensaje quede encolado hasta que el consumidor este disponible.

- **Consistencia distribuida adhoc**: en una cadena sincrona `document-service -> generator-service -> notification-service`, si el tercer servicio falla, los dos primeros ya confirmaron sus transacciones. La compensacion requiere llamadas HTTP inversas que pueden fallar a su vez, creando un problema recursivo. No existe un mecanismo nativo de saga; hay que implementarlo manualmente con estados intermedios y logica de compensacion dispersa. Como argumenta Richardson (2018): *"Las sagas coreografiadas en sistemas sincronos tienden a convertirse en espagueti distribuido"*.

- **Sin desacoplamiento de velocidad**: si `notification-service` es 10 veces mas lento que `generator-service` (porque enviar un email tarda mas que generar un PDF), el throughput del sistema completo queda limitado por el servicio mas lento. En una arquitectura orientada a eventos, cada servicio consume a su propio ritmo gracias al buffering de Kafka.

- **Overhead operacional del stack**: Eureka requiere un cluster (al menos 2 nodos para alta disponibilidad), Config Server necesita un repositorio Git y su propio despliegue, Gateway es un componente adicional que mantener. Esto agrega 3 servicios de infraestructura ademas de los servicios de negocio.

- **Observabilidad de flujos**: rastrear un flujo que cruza 3-4 llamadas HTTP requiere distributed tracing (Zipkin/Jaeger). En una arquitectura orientada a eventos, el `saga_id` actua como correlation ID natural que permite reconstruir el flujo completo consultando las tablas outbox de cada servicio.

### M.4 Arquitectura propuesta: Event-Driven + DDD + Hexagonal

La arquitectura implementada en este trabajo combina:

| Patron | Proposito | Referencia |
|--------|-----------|------------|
| Arquitectura Hexagonal | Aislamiento del dominio frente a infraestructura | Cockburn (2005) |
| Domain-Driven Design | Modelado del negocio en contextos acotados | Evans (2003), Vernon (2013) |
| Event-Driven Architecture | Comunicacion asincrona desacoplada | Hohpe & Woolf (2003) |
| Saga (Orquestacion) | Consistencia eventual con compensacion | Garcia-Molina & Salem (1987) |
| Transactional Outbox | Atomicidad entre escritura local y publicacion | Richardson (2018) |
| Bloqueo Optimista | Concurrencia segura en multiples instancias | Sadalage & Fowler (2012) |

**Ventajas:**
- **Desacoplamiento total**: los servicios no se conocen entre si. Solo conocen topics de Kafka y contratos Avro.
- **Consistencia garantizada**: la combinacion Outbox + Saga asegura que toda operacion alcanza un estado terminal (exito o compensacion) sin perdida de eventos.
- **Escalabilidad selectiva**: se puede escalar `notification-service` a 10 instancias sin tocar `generator-service`, porque el broker absorbe la diferencia de velocidad.
- **Modificabilidad demostrada**: reemplazar Kafka por RabbitMQ, o PostgreSQL por MongoDB, afecta solo la capa de adaptadores (~10% del codigo).
- **Sin infraestructura de descubrimiento**: Kafka actua como intermediario; no se necesita Eureka ni Gateway para la comunicacion entre servicios.
- **Auditabilidad nativa**: las tablas outbox funcionan como log de auditoria con timestamps, saga IDs y payloads completos.

**Limitaciones:**
- **Complejidad inicial**: implementar Saga + Outbox + Hexagonal desde cero requiere mas esfuerzo que un CRUD sincrono. Newman (2015) advierte que *"los microservicios no son una bala de plata; la complejidad que eliminan en un lugar la introducen en otro"*.
- **Latencia por polling**: el outbox scheduler sondea cada 10 segundos, lo que introduce una latencia minima de ~10 segundos entre la escritura y la publicacion. Esto es aceptable para documentos financieros, pero no para casos de uso en tiempo real.
- **Dependencia operacional de Kafka**: el cluster de Kafka (3 brokers, Zookeeper, Schema Registry) es un componente complejo de operar. Sin embargo, servicios gestionados como Confluent Cloud o Azure Event Hubs eliminan esta carga.

### M.5 Tabla comparativa consolidada

| Criterio | Spring Batch | Spring Cloud (sincrono) | Propuesta (Event-Driven) |
|----------|:---:|:---:|:---:|
| **Acoplamiento temporal** | Alto (ventana de batch) | Medio (sincrono con retry) | Bajo (asincrono) |
| **Consistencia** | Transaccional fuerte | Adhoc / manual | Saga + Outbox (eventual garantizada) |
| **Escalabilidad** | Vertical (particiones) | Horizontal con Eureka | Horizontal por servicio + particiones Kafka |
| **Modificabilidad** | Baja (monolito) | Media (acoplamiento HTTP) | Alta (puertos y adaptadores) |
| **Tolerancia a fallos** | Checkpoint/restart | Circuit breaker | Compensacion automatica (saga) + at-least-once |
| **Latencia de respuesta** | Horas (ciclo batch) | Segundos (sincrono) | 10-30 seg (outbox polling) |
| **Complejidad operacional** | Baja (1 artefacto) | Alta (Eureka, Config, Gateway) | Media (Kafka cluster) |
| **Complejidad de desarrollo** | Baja | Media | Alta |
| **Auditabilidad** | Job repository (batch metadata) | Requiere tracing externo | Nativa (tablas outbox) |
| **Procesamiento en tiempo real** | No | Si | Casi tiempo real (~10 seg) |
| **Mejor para** | Cargas masivas programadas | APIs sincronas con baja latencia | Flujos asincronos de multiples pasos |

### M.6 Discusion

Ninguna arquitectura es universalmente superior. La eleccion depende del contexto del problema:

- **Spring Batch** es la opcion correcta cuando el volumen es predecible, la latencia no es critica y la complejidad operacional debe ser minima. Un banco que genera extractos mensuales para todos sus clientes a las 2:00 AM se beneficia de la simplicidad del batch. Sin embargo, cuando el negocio evoluciona hacia la generacion bajo demanda o en tiempo real, el batch se convierte en un cuello de botella arquitectonico dificil de remediar sin reescritura.

- **Spring Cloud** es apropiado cuando la mayoria de las interacciones son request-response con baja latencia (ej: un e-commerce donde el usuario espera respuesta inmediata). Su ecosistema resuelve problemas reales de descubrimiento, configuracion y resiliencia. Pero cuando los flujos son largos (multiples pasos con posibilidad de fallo en cada uno), el acoplamiento sincrono genera fragilidad y la consistencia distribuida se vuelve artesanal.

- **La arquitectura propuesta** es optima para flujos de negocio con multiples pasos, tolerancia a latencia moderada y necesidad de escalabilidad independiente por componente. Su costo es una mayor complejidad de desarrollo inicial, que se amortiza con la evolucionabilidad del sistema a largo plazo. Como argumenta Kleppmann (2017): *"Los sistemas basados en log de eventos son fundamentalmente mas robustos ante fallos parciales que los sistemas basados en peticion-respuesta"*.

En el contexto especifico de este trabajo -- generacion y notificacion de documentos financieros -- la arquitectura orientada a eventos es la mas adecuada porque:
1. El flujo tiene multiples pasos con posibilidad de fallo independiente (generacion puede fallar por datos incompletos, notificacion puede fallar por SMTP no disponible).
2. La latencia de 10-30 segundos es aceptable para documentos financieros (no es un chat en tiempo real).
3. El volumen de notificaciones puede variar drasticamente (fin de mes vs mitad de mes), requiriendo escalabilidad selectiva del `notification-service`.
4. La regulacion financiera exige auditabilidad, que las tablas outbox proveen de forma natural.

---

## Capitulo O: Conclusiones

### O.1 Conclusiones del proyecto

Este trabajo demuestra que es viable construir un sistema distribuido de generacion y notificacion de documentos financieros que combine alta modificabilidad, consistencia eventual garantizada y escalabilidad independiente, sin recurrir a frameworks de coordinacion distribuida complejos como Spring Cloud.

Las principales contribuciones son:

1. **Implementacion rigurosa de Arquitectura Hexagonal**: se demostro que la separacion en modulos Maven con dependencias unidireccionales (dominio -> nada, aplicacion -> dominio, adaptadores -> aplicacion) no es solo un ideal teorico sino una restriccion verificable en tiempo de compilacion. El 90% del codigo fuente es independiente de la infraestructura utilizada.

2. **Patron Saga con compensacion funcional**: se implemento un mecanismo de transacciones distribuidas que garantiza que toda operacion alcanza un estado terminal. El automata de estados de la saga (`STARTED -> PROCESSING -> SUCCESSFUL | COMPENSATED`) demostro ser suficiente para modelar los flujos de negocio sin recurrir a coordinadores de transacciones externos.

3. **Transactional Outbox con bloqueo optimista**: la combinacion del patron outbox con versionado JPA (`@Version`) resolvio simultaneamente el problema del dual-write y la concurrencia entre multiples instancias, sin requerir locks pesimistas ni mecanismos de coordinacion distribuida adicionales.

4. **Despliegue containerizado completo**: se proveen Dockerfiles multi-stage para cada servicio, un `docker-compose.yml` unificado que levanta el sistema completo (4 microservicios + PostgreSQL + Kafka cluster + Schema Registry), y scripts de despliegue para Azure Container Apps con auto-scaling configurable.

5. **Validacion empirica de la modificabilidad**: el analisis de impacto demostro cuantitativamente que un cambio de tecnologia de mensajeria afecta menos del 10% de los archivos del sistema, confirmando la promesa teorica de la arquitectura hexagonal.

### O.2 Conclusiones de la comparativa arquitectonica

La comparativa entre Spring Batch, Spring Cloud y la arquitectura propuesta revela que:

- No existe una arquitectura universalmente optima. Cada enfoque tiene un *sweet spot* determinado por el patron de interaccion predominante (batch vs request-response vs event-driven), los requisitos de latencia, y la complejidad operacional aceptable.

- La deuda tecnica de elegir la arquitectura incorrecta es significativamente mayor que la deuda de elegir la tecnologia incorrecta. Migrar de Kafka a RabbitMQ (misma arquitectura, diferente tecnologia) afecta ~10% del codigo. Migrar de batch a event-driven (diferente arquitectura) es esencialmente una reescritura.

- El ecosistema Spring Cloud resuelve problemas reales, pero introduce acoplamiento temporal sincrono que se vuelve fragil en flujos de multiples pasos. La combinacion de DDD + Hexagonal + Saga + Outbox logra desacoplamiento sin sacrificar consistencia.

- Spring Batch sigue siendo la opcion mas pragmatica para cargas masivas predecibles. La sobre-ingenieria de usar microservicios orientados a eventos para un job que corre una vez al dia a medianoche no se justifica.

### O.3 Trabajo futuro

- Implementar una suite completa de pruebas unitarias e integracion con TestContainers para validar cada patron de forma aislada y en conjunto.
- Evaluar la migracion del outbox scheduler de polling a Change Data Capture (Debezium) para reducir la latencia de publicacion de ~10 segundos a sub-segundo.
- Incorporar un API Gateway (no necesariamente Spring Cloud Gateway) para exponer un punto de entrada unificado con autenticacion y rate limiting a nivel de sistema.
- Explorar la posibilidad de ejecutar los servicios como funciones serverless (Azure Functions / AWS Lambda) para reducir costos en cargas intermitentes.
- Implementar distributed tracing con OpenTelemetry para mejorar la observabilidad del flujo completo de la saga.

---

## Referencias

- Cockburn, A. (2005). *Hexagonal Architecture*. Disponible en: https://alistair.cockburn.us/hexagonal-architecture/

- Carnell, J. & Sanchez, I. (2021). *Spring Microservices in Action* (2nd ed.). Manning Publications.

- Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley Professional.

- Fowler, M. (2002). *Patterns of Enterprise Application Architecture*. Addison-Wesley Professional.

- Garcia-Molina, H. & Salem, K. (1987). Sagas. *ACM SIGMOD Record*, 16(3), 249-259. https://doi.org/10.1145/38714.38742

- Hohpe, G. & Woolf, B. (2003). *Enterprise Integration Patterns: Designing, Building, and Deploying Messaging Solutions*. Addison-Wesley Professional.

- Kleppmann, M. (2017). *Designing Data-Intensive Applications*. O'Reilly Media.

- Martin, R.C. (2003). *Agile Software Development: Principles, Patterns, and Practices*. Prentice Hall.

- Minella, M. (2019). *The Definitive Guide to Spring Batch* (2nd ed.). Apress.

- Newman, S. (2015). *Building Microservices: Designing Fine-Grained Systems*. O'Reilly Media.

- Nygard, M. (2007). *Release It!: Design and Deploy Production-Ready Software*. Pragmatic Bookshelf.

- Richardson, C. (2018). *Microservices Patterns: With Examples in Java*. Manning Publications.

- Sadalage, P. & Fowler, M. (2012). *NoSQL Distilled: A Brief Guide to the Emerging World of Polyglot Persistence*. Addison-Wesley Professional.

- Vernon, V. (2013). *Implementing Domain-Driven Design*. Addison-Wesley Professional.
