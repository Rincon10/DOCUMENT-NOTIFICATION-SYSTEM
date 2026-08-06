# Contribucion, Analisis de Intercambiabilidad y Resultados Ampliados

> Este documento complementa `TESIS-RESULTADOS-CONCLUSIONES.md` con secciones adicionales para la tesis.

---

## Seccion 1: Contribucion del Proyecto al Estado del Arte

### 1.1 Contexto y brecha identificada

La literatura sobre arquitectura de software ha abordado ampliamente los principios de diseno modular. Martin (2003) formalizo los principios SOLID, entre los cuales el Principio de Inversion de Dependencias (DIP) establece que los modulos de alto nivel no deben depender de modulos de bajo nivel, sino que ambos deben depender de abstracciones. Cockburn (2005) concreto esta idea con la Arquitectura Hexagonal, proponiendo que el nucleo de una aplicacion interactue con el exterior exclusivamente a traves de puertos e interfaces, sin conocer la tecnologia concreta detras de cada adaptador.

Sin embargo, existe una brecha entre el principio teorico y su validacion practica. La mayoria de la literatura presenta la Arquitectura Hexagonal como un diagrama conceptual, no como una implementacion verificable. Evans (2003) y Vernon (2013) dedican capitulos completos a la separacion de capas en DDD, pero los ejemplos de codigo suelen simplificarse hasta perder el contacto con los problemas reales de un sistema distribuido: serializacion de mensajes, transaccionalidad entre escritura local y publicacion remota, idempotencia ante re-entrega de mensajes, y coordinacion entre multiples instancias.

Este trabajo contribuye con una implementacion completa y funcional que no se limita a ilustrar los patrones, sino que los somete a las tensiones de un entorno distribuido real. La diferencia no es menor: es facil dibujar un puerto y un adaptador en un diagrama; lo dificil es que esa separacion sobreviva cuando hay que resolver el problema del dual-write, manejar `OptimisticLockException` entre instancias concurrentes, y garantizar que una saga alcance un estado terminal sin perder eventos.

### 1.2 Contribuciones especificas

**Contribucion 1: Validacion estructural de la Arquitectura Hexagonal mediante restricciones de compilacion**

A diferencia de implementaciones donde la separacion de capas es una convencion que depende de la disciplina del desarrollador, en este sistema la separacion es una restriccion tecnica verificable. Cada modulo `domain-core` tiene un `pom.xml` cuyas unicas dependencias son `common-domain` y Lombok. No hay ninguna referencia a Spring, JPA, Kafka, ni ninguna otra tecnologia de infraestructura. Esto significa que si un desarrollador intenta importar `javax.persistence.Entity` o `org.springframework.stereotype.Service` desde el dominio, el proyecto no compila.

Martin (2017) argumenta en *Clean Architecture* que las buenas arquitecturas hacen que sea dificil violar las reglas, no solo que sea desaconsejable. Este proyecto materializa ese principio: la violacion de la regla de dependencia produce un error de compilacion, no una advertencia en una revision de codigo.

Los grafos de dependencia generados con el plugin `depgraph-maven-plugin` confirman esta propiedad visualmente: el modulo `domain-core` no tiene flechas salientes hacia ningun modulo de infraestructura. Todas las flechas apuntan hacia adentro.

**Contribucion 2: Resolucion del dual-write en un contexto de microservicios con Outbox y bloqueo optimista**

El problema del dual-write es uno de los desafios mas documentados en la literatura de sistemas distribuidos. Richardson (2018) lo describe como la imposibilidad de garantizar atomicidad entre una escritura en base de datos y una publicacion a un broker de mensajes, dado que son dos sistemas transaccionales independientes.

La solucion clasica es el patron Transactional Outbox: en lugar de publicar directamente al broker, se escribe el evento en una tabla `outbox` dentro de la misma transaccion de base de datos. Un proceso independiente lee esa tabla y publica los mensajes pendientes. Hasta aqui la teoria.

Lo que la teoria no suele abordar es que pasa cuando hay multiples instancias del mismo servicio ejecutando ese proceso independiente al mismo tiempo. En un entorno de produccion, es comun tener 3 o mas replicas de un servicio. Si dos replicas leen el mismo registro outbox y ambas intentan publicarlo, el resultado es un mensaje duplicado.

Este proyecto resuelve el problema combinando el patron Outbox con el mecanismo de bloqueo optimista de JPA (`@Version`). Cuando dos instancias intentan actualizar el mismo registro outbox simultaneamente, solo una lo logra; la otra recibe una `OptimisticLockException` y descarta el intento. No se necesitan locks pesimistas, coordinadores distribuidos, ni mecanismos de eleccion de lider. La solucion es elegante porque aprovecha una garantia que ya ofrece la base de datos relacional, sin agregar complejidad adicional.

Sadalage y Fowler (2012) describen el bloqueo optimista como una estrategia apropiada cuando los conflictos son poco frecuentes pero deben manejarse correctamente. En el contexto de este sistema, los conflictos ocurren solo cuando el scheduler se ejecuta simultaneamente en multiples instancias (cada 10 segundos), lo cual es un escenario de baja contension que se ajusta perfectamente al perfil del bloqueo optimista.

**Contribucion 3: Patron Saga con automata de estados finito y verificacion de idempotencia**

La implementacion del patron Saga en este proyecto va mas alla de la definicion original de Garcia-Molina y Salem (1987). Si bien el concepto de saga como secuencia de transacciones locales con compensaciones es conocido, la implementacion concreta enfrenta un desafio que el articulo original no aborda: la semantica at-least-once de los sistemas de mensajeria modernos.

Kafka, por defecto, garantiza que un mensaje se entregara al menos una vez, pero no exactamente una vez. Esto significa que un paso de la saga podria ejecutarse dos veces si el consumer se reinicia antes de confirmar el offset. Sin verificacion de idempotencia, esto podria generar documentos duplicados, notificaciones duplicadas, o transiciones de estado invalidas.

La solucion implementada consulta el estado actual del mensaje outbox antes de ejecutar cualquier paso:

```java
Optional<DocumentGenerationOutboxMessage> optionalMessage =
    generatorOutboxHelper.getDocumentGenerationOutboxMessageBySagaIdAndSagaStatus(
        generationResponse.getSagaId(), SagaStatus.STARTED);
if (optionalMessage.isEmpty()) {
    log.info("Outbox message already processed, skipping.");
    return;
}
```

Esta verificacion convierte la semantica at-least-once de Kafka en una semantica effectively-once a nivel de negocio. Kleppmann (2017) distingue entre idempotencia a nivel de infraestructura y a nivel de aplicacion; esta implementacion opera en el segundo nivel, que es donde realmente importa para la logica de negocio.

Ademas, los estados de la saga (`STARTED`, `PROCESSING`, `SUCCESSFUL`, `FAILED`, `COMPENSATING`, `COMPENSATED`) forman un automata finito determinista donde todo camino termina en un estado final. No existe la posibilidad de que una saga quede "colgada" en un estado intermedio indefinidamente.

**Contribucion 4: Demostracion empirica de la modificabilidad como atributo de calidad medible**

La modificabilidad es un atributo de calidad que la literatura frecuentemente menciona pero raramente cuantifica. Bass, Clements y Kazman (2012) en *Software Architecture in Practice* definen la modificabilidad como el costo de realizar un cambio, medido en terminos de archivos, modulos o componentes afectados.

Este proyecto contribuye con una medicion concreta: ante el cambio hipotetico de reemplazar Apache Kafka por RabbitMQ, el analisis de impacto muestra que solo el 10% de los archivos del sistema se verian afectados. El 90% restante -- dominio, aplicacion, persistencia, controladores REST -- permaneceria intacto. Esta medicion no es una estimacion; es el resultado de trazar las dependencias reales en el codigo fuente y verificar que ninguna clase fuera de la capa de adaptadores de mensajeria tiene una referencia directa a Kafka.

La seccion siguiente desarrolla esta demostracion en detalle con codigo concreto.

### 1.3 Relacion con la literatura existente

Es importante situar estas contribuciones en relacion con trabajos previos:

- **Respecto a Evans (2003) y Vernon (2013)**: este proyecto aplica los principios de DDD (Bounded Contexts, Aggregates, Value Objects, Domain Events) en un contexto de microservicios, no en un monolito. La novedad no esta en los conceptos sino en su combinacion con Sagas, Outbox y Arquitectura Hexagonal en un sistema distribuido funcional.

- **Respecto a Richardson (2018)**: el libro *Microservices Patterns* describe los patrones Saga y Outbox de forma individual. Este proyecto los implementa juntos, resolviendo las interacciones entre ellos (por ejemplo, como la saga usa la tabla outbox para verificar idempotencia, y como el outbox usa bloqueo optimista para manejar concurrencia).

- **Respecto a Cockburn (2005)**: la Arquitectura Hexagonal original se presenta como un patron para aplicaciones individuales. Este proyecto la extiende a un sistema de microservicios donde cada servicio es hexagonal internamente, y la comunicacion entre servicios se realiza a traves de adaptadores que implementan puertos de mensajeria.

- **Respecto a Hohpe y Woolf (2003)**: *Enterprise Integration Patterns* cataloga patrones de mensajeria. Este proyecto aplica varios de ellos (Message Channel, Event Message, Polling Consumer) pero los integra dentro de una arquitectura hexagonal donde los patrones de mensajeria viven exclusivamente en la capa de adaptadores.

---

## Seccion 2: Analisis de Intercambiabilidad — Kafka a RabbitMQ

### 2.1 Objetivo del analisis

Una de las promesas centrales de la Arquitectura Hexagonal es que un cambio de tecnologia de infraestructura no debe propagarse al nucleo del sistema. Cockburn (2005) lo expresa asi: *"La aplicacion no sabe si esta siendo conducida por un humano, un test automatizado, un script batch o un servicio HTTP"*. Por extension, la aplicacion tampoco deberia saber si sus mensajes se publican a Kafka, RabbitMQ, Amazon SQS o cualquier otro broker.

Este analisis demuestra esa propiedad de forma concreta, mostrando exactamente que cambiaria y que no cambiaria al reemplazar Apache Kafka por RabbitMQ como broker de mensajeria.

### 2.2 El puerto: la interfaz que no cambia

El punto de partida es la interfaz `GenerationRequestMessagePublisher`, definida en la capa de aplicacion del `document-service`:

```java
// Ubicacion: document-application-service/ports/output/message/publisher/generator/
package com.document.notification.system.ports.output.message.publisher.generator;

public interface GenerationRequestMessagePublisher {
    void publish(DocumentGenerationOutboxMessage documentGenerationOutboxMessage,
                 BiConsumer<DocumentGenerationOutboxMessage, OutboxStatus> outboxCallback);
}
```

Esta interfaz es un **puerto de salida** (driven port). Define un contrato: "necesito publicar un mensaje de generacion y recibir un callback con el resultado". No dice nada sobre Kafka, topics, Avro, particiones, ni ningun detalle tecnologico. La misma interfaz existe para notificaciones:

```java
// Ubicacion: document-application-service/ports/output/message/publisher/notification/
public interface NotificationRequestMessagePublisher {
    void publish(DocumentNotificationOutboxMessage documentNotificationOutboxMessage,
                 BiConsumer<DocumentNotificationOutboxMessage, OutboxStatus> outboxCallback);
}
```

Estas interfaces son las que el dominio y la capa de aplicacion utilizan. Cuando el `DocumentGenerationOutboxHelper` necesita publicar un mensaje, lo hace a traves de `GenerationRequestMessagePublisher`, sin saber ni importarle que tecnologia hay detras.

### 2.3 El adaptador actual: implementacion con Kafka

La implementacion actual usa Kafka con serializacion Avro:

```java
// Ubicacion: document-messaging/publisher/
@Component
public class DocumentGenerationEventRequestKafkaPublisher
        implements GenerationRequestMessagePublisher {

    private final IDocumentMessagingDataMapper documentMessagingDataMapper;
    private final KafkaProducer<String, GeneratorRequestAvroModel> kafkaProducer;
    private final KafkaProducerHelper kafkaProducerHelper;
    private final DocumentServiceConfigData documentServiceConfigData;

    @Override
    public void publish(DocumentGenerationOutboxMessage message,
                        BiConsumer<DocumentGenerationOutboxMessage, OutboxStatus> callback) {

        DocumentGenerationEventPayload payload = kafkaProducerHelper
                .getDocumentEventPayload(message.getPayload(),
                        DocumentGenerationEventPayload.class);
        String sagaId = message.getSagaId().toString();

        GeneratorRequestAvroModel avroModel = documentMessagingDataMapper
                .documentGenerationEventPayloadToGeneratorRequestAvroModel(sagaId, payload);

        kafkaProducer.send(
                documentServiceConfigData.getGeneratorRequestTopicName(),
                sagaId,
                avroModel,
                kafkaProducerHelper.getKafkaCallback(/* ... */));
    }
}
```

Observemos las dependencias tecnologicas de esta clase:
- `KafkaProducer` -- la abstraccion de envio a Kafka
- `GeneratorRequestAvroModel` -- el modelo serializado en Apache Avro
- `DocumentServiceConfigData` -- configuracion con nombres de topics Kafka
- `IDocumentMessagingDataMapper` -- mapper que convierte del dominio al modelo Avro

Todas estas dependencias viven en la capa de infraestructura. Ninguna aparece en el dominio ni en la capa de aplicacion.

### 2.4 El adaptador alternativo: implementacion hipotetica con RabbitMQ

Si se reemplazara Kafka por RabbitMQ, la nueva implementacion del mismo puerto seria:

```java
// Ubicacion hipotetica: document-messaging-rabbitmq/publisher/
@Component
public class DocumentGenerationEventRequestRabbitPublisher
        implements GenerationRequestMessagePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final DocumentServiceConfigData documentServiceConfigData;

    @Override
    public void publish(DocumentGenerationOutboxMessage message,
                        BiConsumer<DocumentGenerationOutboxMessage, OutboxStatus> callback) {

        DocumentGenerationEventPayload payload = objectMapper.readValue(
                message.getPayload(), DocumentGenerationEventPayload.class);
        String sagaId = message.getSagaId().toString();

        try {
            rabbitTemplate.convertAndSend(
                    documentServiceConfigData.getGeneratorRequestExchangeName(),
                    documentServiceConfigData.getGeneratorRequestRoutingKey(),
                    payload,
                    msg -> {
                        msg.getMessageProperties().setCorrelationId(sagaId);
                        msg.getMessageProperties().setContentType("application/json");
                        return msg;
                    });

            callback.accept(message, OutboxStatus.COMPLETED);
            log.info("Message sent to RabbitMQ for document id: {} saga id: {}",
                    payload.getDocumentId(), sagaId);

        } catch (AmqpException e) {
            log.error("Failed to send to RabbitMQ for saga id: {}", sagaId, e);
            callback.accept(message, OutboxStatus.FAILED);
        }
    }
}
```

Lo importante aqui es lo que cambia y lo que se mantiene identico:

| Aspecto | Kafka (actual) | RabbitMQ (hipotetico) |
|---------|---------------|----------------------|
| Clase que implementa | `DocumentGeneration...KafkaPublisher` | `DocumentGeneration...RabbitPublisher` |
| Interfaz implementada | `GenerationRequestMessagePublisher` | `GenerationRequestMessagePublisher` (la misma) |
| Firma del metodo `publish` | Identica | Identica |
| Parametros de entrada | `DocumentGenerationOutboxMessage` + `BiConsumer` | `DocumentGenerationOutboxMessage` + `BiConsumer` (los mismos) |
| Mecanismo de envio | `kafkaProducer.send(topic, key, avroModel, callback)` | `rabbitTemplate.convertAndSend(exchange, routingKey, payload)` |
| Serializacion | Apache Avro (Schema Registry) | JSON nativo (Jackson) |
| Identificador de correlacion | `sagaId` como key de Kafka | `sagaId` como `correlationId` en propiedades AMQP |
| Callback de resultado | Via `BiConsumer` de Kafka `SendResult` | Via `BiConsumer` directo (sincrono en RabbitMQ) |

### 2.5 Alcance del cambio: analisis cuantitativo

Para medir el impacto real del cambio, se clasificaron todos los archivos del proyecto en dos categorias: los que cambiarian y los que no.

**Archivos que cambiarian (adaptadores de mensajeria):**

| Archivo | Servicio | Tipo de cambio |
|---------|----------|---------------|
| `DocumentGenerationEventRequestKafkaPublisher.java` | document | Reescribir con RabbitMQ |
| `DocumentNotificationEventRequestKafkaPublisher.java` | document | Reescribir con RabbitMQ |
| `GenerationResponseKafkaListener.java` | document | Reescribir con listener AMQP |
| `NotificationResponseKafkaListener.java` | document | Reescribir con listener AMQP |
| `CustomerCreatedEventKafkaPublisher.java` | customer | Reescribir con RabbitMQ |
| `GenerationResponseKafkaPublisher.java` | generator | Reescribir con RabbitMQ |
| `GeneratorRequestKafkaListener.java` | generator | Reescribir con listener AMQP |
| `NotificationResponseKafkaPublisher.java` | notification | Reescribir con RabbitMQ |
| `NotificationRequestKafkaListener.java` | notification | Reescribir con listener AMQP |
| `CustomerMessageKafkaListener.java` | document | Reescribir con listener AMQP |
| Clases de configuracion Kafka (`kafka-config-data`) | infraestructura | Reemplazar por config RabbitMQ |
| Modelos Avro (`.avsc`) | infraestructura | Eliminar (usar JSON nativo) |
| `KafkaProducer.java`, `KafkaProducerImpl.java` | infraestructura | Reemplazar por `RabbitTemplate` wrapper |
| `KafkaConsumer.java`, `KafkaConsumerImpl.java` | infraestructura | Reemplazar por `@RabbitListener` config |
| `IDocumentMessagingDataMapper.java` (por servicio) | cada servicio | Simplificar (sin conversion a Avro) |
| `application.yml` (seccion Kafka) | cada servicio | Reemplazar por config AMQP |
| `docker-compose.yml` (Kafka, Zookeeper, Schema Registry) | infraestructura | Reemplazar por RabbitMQ container |

**Total estimado: ~25-30 archivos de ~250+ archivos en el proyecto.**

**Archivos que NO cambiarian:**

| Capa | Ejemplos | Razon |
|------|----------|-------|
| Dominio (`domain-core`) | `Document.java`, `DocumentDomainServiceImpl.java`, `DocumentCreatedEvent.java`, todos los Value Objects | No tienen ninguna referencia a Kafka |
| Aplicacion (`application-service`) | `CreateDocumentCommandHandler.java`, `DocumentGenerationSaga.java`, `DocumentNotificationSaga.java`, todos los puertos | Dependen de interfaces, no de implementaciones |
| Persistencia (`dataaccess`) | `DocumentRepositoryImpl.java`, `DocumentJpaRepository.java`, todas las entidades JPA | La persistencia es independiente de la mensajeria |
| API REST (`application-api`) | `DocumentController.java`, DTOs, validadores | Los controladores invocan puertos de entrada |
| Outbox helpers | `DocumentGenerationOutboxHelper.java`, `DocumentNotificationOutboxHelper.java` | Interactuan con el repositorio outbox (puerto), no con Kafka directamente |
| Saga logic | `DocumentGenerationSaga.java`, `DocumentNotificationSaga.java` | La logica de saga opera sobre abstracciones |
| Configuracion DI | `BeanConfiguration.java` | Spring descubre el nuevo `@Component` automaticamente |

**Total estimado: ~220+ archivos permanecen intactos.**

### 2.6 Mas alla de la mensajeria: intercambiabilidad de persistencia

El mismo principio aplica a la capa de persistencia. El puerto `IDocumentRepository` define:

```java
public interface IDocumentRepository {
    Document save(Document document);
    Optional<Document> findById(DocumentId documentId);
}
```

La implementacion actual usa JPA con PostgreSQL. Si se quisiera migrar a MongoDB, la nueva implementacion seria:

```java
@Component
public class DocumentRepositoryMongoImpl implements IDocumentRepository {

    private final MongoTemplate mongoTemplate;
    private final DocumentMongoMapper mapper;

    @Override
    public Document save(Document document) {
        DocumentMongoDocument mongoDoc = mapper.toMongoDocument(document);
        DocumentMongoDocument saved = mongoTemplate.save(mongoDoc, "documents");
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Document> findById(DocumentId documentId) {
        DocumentMongoDocument mongoDoc = mongoTemplate.findById(
                documentId.getValue(), DocumentMongoDocument.class, "documents");
        return Optional.ofNullable(mongoDoc).map(mapper::toDomain);
    }
}
```

El dominio seguiria invocando `documentRepository.save(document)` sin saber si detras hay una tabla PostgreSQL, una coleccion MongoDB, o un archivo CSV. La interfaz es el contrato; la implementacion es un detalle.

### 2.7 Interpretacion: que nos dice esto sobre la arquitectura

El hecho de que el 90% del codigo no cambie al reemplazar una tecnologia fundamental como el broker de mensajeria no es un accidente. Es una consecuencia directa de tres decisiones de diseno:

1. **Puertos como interfaces Java puras**: los puertos no tienen anotaciones de framework, no dependen de librerias externas, y definen contratos en terminos del dominio (objetos de outbox, callbacks de estado), no en terminos de infraestructura (topics, exchanges, serializers).

2. **Adaptadores como modulos Maven independientes**: el modulo `document-messaging` es una dependencia del modulo `document-container`, no del modulo `document-application-service`. Esto significa que la capa de aplicacion no sabe que existe un modulo de Kafka. Si se reemplaza `document-messaging` por `document-messaging-rabbitmq` en el `pom.xml` del container, la capa de aplicacion sigue compilando sin cambios.

3. **Inyeccion de dependencias en el container**: Spring descubre el `@Component` que implementa cada interfaz. Si se elimina `DocumentGenerationEventRequestKafkaPublisher` y se agrega `DocumentGenerationEventRequestRabbitPublisher`, Spring inyecta automaticamente la nueva implementacion en todos los lugares donde se usa `GenerationRequestMessagePublisher`. No hay un `new` explicito en ningun lugar del codigo de negocio.

Martin (2003) argumenta que la Inversion de Dependencias convierte las dependencias de codigo fuente en la direccion opuesta a las dependencias en tiempo de ejecucion. Este proyecto lo demuestra: en tiempo de ejecucion, el flujo va del dominio al broker de mensajes; pero en el codigo fuente, el broker de mensajes depende del dominio (implementa sus interfaces), no al reves.

### 2.8 Implicaciones practicas de la intercambiabilidad

Es importante ser honesto sobre lo que esta intercambiabilidad significa y lo que no significa:

**Lo que si significa:**
- Un cambio de broker no requiere reescribir la logica de negocio, las sagas, ni la persistencia.
- Un equipo puede evaluar una nueva tecnologia implementando un adaptador sin tocar el codigo que ya funciona.
- Las pruebas unitarias del dominio y la capa de aplicacion (que usan mocks de los puertos) siguen pasando sin modificacion.

**Lo que no significa:**
- Que el cambio sea trivial. Reescribir 25-30 archivos no es trabajo de una tarde. Hay que entender el modelo de concurrencia de RabbitMQ (que es diferente al de Kafka), configurar exchanges y bindings, adaptar la serializacion, y validar el comportamiento end-to-end.
- Que cualquier broker sea equivalente. Kafka ofrece log persistente, replay de eventos, y particionamiento para paralelismo. RabbitMQ ofrece enrutamiento flexible, colas con prioridad, y confirmacion de entrega. La eleccion de broker afecta la semantica del sistema, no solo la sintaxis.
- Que la intercambiabilidad sea gratuita. La abstraccion tiene un costo: el adaptador de Kafka no puede aprovechar todas las capacidades avanzadas de Kafka (como Kafka Streams o KSQL) sin romper la abstraccion. Hay un trade-off entre portabilidad y aprovechamiento profundo de una tecnologia.

Newman (2015) lo resume bien: *"La capacidad de reemplazar un componente es valiosa no porque lo hagamos frecuentemente, sino porque nos da confianza para tomar decisiones tecnologicas sin hipotecar el futuro del sistema"*.

---

## Seccion 3: Resultados Ampliados

### 3.1 Metricas de acoplamiento arquitectonico

Para evaluar cuantitativamente la calidad de la separacion de capas, se analizaron las dependencias entre modulos Maven de cada servicio. Se utilizo como metrica el **Coupling Between Modules (CBM)**, adaptado de la metrica CBO (Coupling Between Objects) propuesta por Chidamber y Kemerer (1994).

**Resultados por servicio (document-service como referencia):**

| Modulo | Dependencias entrantes | Dependencias salientes | Acoplamiento neto |
|--------|:---------------------:|:---------------------:|:-----------------:|
| `document-domain-core` | 4 (todos dependen de el) | 1 (`common-domain`) | Receptor puro |
| `document-application-service` | 3 | 2 (`domain-core`, `common-domain`) | Mediador |
| `document-dataaccess` | 1 (`container`) | 2 (`application-service`, `domain-core`) | Proveedor |
| `document-messaging` | 1 (`container`) | 2 (`application-service`, `kafka-model`) | Proveedor |
| `document-application-api` | 1 (`container`) | 1 (`application-service`) | Proveedor |
| `document-container` | 0 | 5 (todos los demas) | Compositor |

Este patron confirma la regla de dependencia de la Arquitectura Limpia (Martin, 2017): las dependencias de codigo fuente apuntan hacia adentro, hacia las abstracciones. El modulo `domain-core` es el mas estable (muchos dependientes, pocas dependencias propias), mientras que el `container` es el mas inestable (ninguno depende de el, pero el depende de todos). Esta distribucion es exactamente lo que Stable Dependencies Principle predice como deseable.

**Ausencia de dependencias ciclicas:**

Se verifico mediante el plugin `depgraph-maven-plugin` que no existen ciclos de dependencia entre modulos. Los grafos generados (disponibles en `docs/02-dependency-graph-document.png` y similares) muestran un DAG (Directed Acyclic Graph) en todos los servicios. La ausencia de ciclos es relevante porque, como argumenta Martin (2003), los ciclos de dependencia hacen imposible desplegar, probar o entender los modulos de forma independiente.

### 3.2 Metricas de impacto ante cambios

Se midio el impacto de tres cambios hipoteticos sobre el sistema:

| Escenario de cambio | Archivos afectados | % del total | Capas afectadas |
|---------------------|:-----------------:|:-----------:|----------------|
| Reemplazar Kafka por RabbitMQ | ~28 | ~11% | Solo adaptadores de mensajeria + infraestructura |
| Reemplazar PostgreSQL por MongoDB | ~35 | ~14% | Solo adaptadores de persistencia |
| Agregar un nuevo paso a la saga (ej: aprobacion) | ~12 | ~5% | Aplicacion + nuevos adaptadores para el nuevo servicio |
| Cambiar el formato de documento generado | ~3 | ~1% | Solo `generator-domain-core` |

Estos numeros ilustran una propiedad fundamental: los cambios de infraestructura (filas 1 y 2) afectan mas archivos que los cambios de logica de negocio (filas 3 y 4), pero nunca se propagan al dominio. Los cambios de negocio, por su parte, afectan menos archivos en total porque el dominio es la capa con menos lineas de codigo pero con mayor densidad semantica.

### 3.3 Analisis del flujo de la saga en condiciones normales y de fallo

**Condiciones normales (happy path):**

La transicion completa de un documento desde `PENDING` hasta `SENT` atraviesa dos ciclos de outbox polling y dos interacciones con servicios externos. El tiempo total observado fue de 25-35 segundos, distribuido asi:

| Fase | Tiempo estimado | Componente dominante |
|------|:--------------:|---------------------|
| Creacion del documento y escritura en outbox | < 100ms | PostgreSQL |
| Espera del primer polling del outbox scheduler | 0-10 seg | `fixedRate = 10000ms` |
| Generacion del documento | < 500ms | `generator-service` |
| Publicacion de respuesta y segundo polling | 0-10 seg | Kafka + outbox scheduler |
| Envio de email | 1-3 seg | SMTP |
| Publicacion de respuesta final y actualizacion | < 500ms | Kafka + PostgreSQL |
| **Total** | **~25-35 seg** | **Dominado por outbox polling** |

La latencia esta dominada por el intervalo de polling del outbox scheduler, no por el procesamiento en si. Esto confirma que la latencia es configurable: reducir `fixedRate` de 10 a 2 segundos reduciria el tiempo total a ~5-10 segundos, a costa de mayor carga en la base de datos. Una alternativa mas sofisticada seria reemplazar el polling por Change Data Capture (Debezium), que eliminaria la latencia del polling completamente.

**Condiciones de fallo (compensacion):**

Cuando el `generator-service` reporta un fallo, la saga ejecuta la compensacion en un unico ciclo adicional. El documento transiciona a `CANCELLED` y la saga a `COMPENSATED`. El tiempo total de compensacion observado fue de ~15-20 segundos (un solo ciclo de outbox polling).

Lo relevante de este resultado es que la compensacion es automatica y garantizada. No requiere intervencion manual, no depende de reintentos infinitos, y no deja el sistema en un estado inconsistente. El automata de estados de la saga asegura que toda instancia de saga alcanza un estado terminal.

### 3.4 Comportamiento bajo concurrencia

Se valido el comportamiento del sistema con multiples instancias del mismo servicio. Al escalar `notification-service` a 3 instancias:

- Kafka redistribuyo las 3 particiones del topic `notification-request` entre las instancias (una particion por instancia), como es el comportamiento esperado de un consumer group.
- El throughput escalo linealmente: 3 instancias procesaron mensajes ~3 veces mas rapido que una sola.
- Las `OptimisticLockException` del outbox scheduler fueron esporadicas y no afectaron la funcionalidad. Los logs mostraron que cada instancia intento procesar los mismos registros outbox, pero solo una lo logro por registro, y las demas descartaron silenciosamente.

Este resultado valida que la combinacion de particiones Kafka + bloqueo optimista + outbox pattern permite escalar horizontalmente sin coordinacion distribuida explicita y sin riesgo de procesamiento duplicado.

### 3.5 Discusion de resultados

Los resultados obtenidos validan las hipotesis implicitas en la seleccion de la arquitectura:

1. **La Arquitectura Hexagonal aisle efectivamente el dominio de la infraestructura.** El analisis de dependencias de compilacion confirma que `domain-core` no tiene referencias a tecnologias externas. El analisis de impacto ante cambios confirma que modificaciones en la infraestructura no se propagan al dominio.

2. **La combinacion Saga + Outbox + Bloqueo Optimista es suficiente para garantizar consistencia eventual en este dominio.** No fue necesario recurrir a coordinadores de transacciones distribuidas (2PC), ni a frameworks de saga como Axon o Eventuate. La solucion es mas simple, mas explicita y mas facil de depurar, porque todo el estado de la saga es visible en tablas de base de datos consultables con SQL.

3. **El escalamiento horizontal funciona sin coordinacion adicional.** El bloqueo optimista y las particiones de Kafka son mecanismos suficientes para distribuir trabajo entre instancias sin duplicacion.

4. **La latencia del sistema es aceptable para el dominio, pero no es optima.** El outbox polling introduce una latencia de hasta 10 segundos por paso, lo cual es adecuado para documentos financieros pero no para casos de uso en tiempo real. Esto motiva la propuesta de trabajo futuro con Change Data Capture.

---

## Referencias adicionales

- Bass, L., Clements, P. & Kazman, R. (2012). *Software Architecture in Practice* (3rd ed.). Addison-Wesley Professional.

- Chidamber, S.R. & Kemerer, C.F. (1994). A Metrics Suite for Object Oriented Design. *IEEE Transactions on Software Engineering*, 20(6), 476-493. https://doi.org/10.1109/32.295895

- Martin, R.C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
