# Notification Service

Microservicio encargado de enviar notificaciones (email) a los clientes cuando sus documentos han sido generados exitosamente.

## Multiple Instancias

El servicio soporta ejecucion de multiples instancias para escalar horizontalmente. Todas las instancias pueden correr con la misma configuracion gracias a:

- **Kafka Consumer Group**: Todas comparten el mismo `notification-consumer-group-id`, Kafka reparte las particiones automaticamente entre ellas.
- **Outbox Scheduler con Pessimistic Locking**: Los queries del outbox usan `SELECT FOR UPDATE SKIP LOCKED`, lo que garantiza que cada instancia tome mensajes distintos sin duplicados ni bloqueos entre si.

### Instancia 1
```bash
java -jar notification-container.jar
```

### Instancia 2
```bash
java -jar notification-container.jar \
  --server.port=8185 \
  --notification-service.instance-id=notification-2 \
  --spring.sql.init.mode=never
```

### Instancia 3
```bash
java -jar notification-container.jar \
  --server.port=8186 \
  --notification-service.instance-id=notification-3 \
  --spring.sql.init.mode=never
```

O usando variables de entorno:
```bash
SERVER_PORT=8185 \
NOTIFICATION_INSTANCE_ID=notification-2 \
SQL_INIT_MODE=never \
java -jar notification-container.jar
```

**Reglas para multiples instancias:**
- Solo **una instancia** debe ejecutar `spring.sql.init.mode=always` (la primera). Las demas deben usar `never` para no recrear el schema en cada arranque.
- Todas las instancias pueden tener el **outbox scheduler activo** — el pessimistic locking (`SKIP LOCKED`) evita que dos instancias procesen el mismo mensaje.
- El `instance-id` identifica cada instancia en los logs.

## Propiedades de Configuracion

### Server

| Propiedad | Valor por defecto | Descripcion |
|-----------|-------------------|-------------|
| `server.port` | `8183` | Puerto en el que corre el servicio |

### Notification Service

| Propiedad | Valor por defecto | Descripcion |
|-----------|-------------------|-------------|
| `notification-service.notification-request-topic-name` | `notification-request` | Nombre del topic de Kafka donde se reciben las solicitudes de notificacion |
| `notification-service.notification-response-topic-name` | `notification-response` | Nombre del topic de Kafka donde se publican las respuestas de notificacion |
| `notification-service.outbox-scheduler-fixed-rate` | `10000` | Intervalo en ms entre cada ejecucion del scheduler de outbox |
| `notification-service.outbox-scheduler-initial-delay` | `10000` | Delay inicial en ms antes de la primera ejecucion del scheduler de outbox |

### Email - Configuracion de envio

| Propiedad | Env Variable | Valor por defecto | Descripcion |
|-----------|-------------|-------------------|-------------|
| `notification-service.mail.from` | `MAIL_FROM` | `informaticapruebastesis@gmail.com` | Direccion de correo remitente |
| `spring.mail.host` | `MAIL_HOST` | `smtp.gmail.com` | Servidor SMTP |
| `spring.mail.port` | `MAIL_PORT` | `587` | Puerto SMTP (587 para TLS) |
| `spring.mail.username` | `MAIL_USERNAME` | `informaticapruebastesis@gmail.com` | Usuario de autenticacion SMTP |
| `spring.mail.password` | `MAIL_PASSWORD` | _(vacio)_ | Password o App Password de Gmail |

### Email - SMTP Properties

| Propiedad | Env Variable | Valor por defecto | Descripcion |
|-----------|-------------|-------------------|-------------|
| `mail.smtp.auth` | `MAIL_SMTP_AUTH` | `true` | Habilita autenticacion SMTP |
| `mail.smtp.starttls.enable` | `MAIL_SMTP_STARTTLS_ENABLE` | `true` | Habilita STARTTLS para conexion segura |
| `mail.smtp.starttls.required` | `MAIL_SMTP_STARTTLS_REQUIRED` | `true` | Obliga el uso de STARTTLS, falla si no esta disponible |
| `mail.smtp.connectiontimeout` | `MAIL_SMTP_CONNECTION_TIMEOUT` | `5000` | Timeout en ms para establecer la conexion con el servidor SMTP |
| `mail.smtp.timeout` | `MAIL_SMTP_TIMEOUT` | `5000` | Timeout en ms para lectura de respuesta del servidor SMTP |
| `mail.smtp.writetimeout` | `MAIL_SMTP_WRITE_TIMEOUT` | `5000` | Timeout en ms para escritura de datos al servidor SMTP |

### Email - Rate Limiting (Token Bucket)

Controla la velocidad de envio de correos para no saturar el servidor SMTP. Usa el patron **Token Bucket**: se dispone de un numero fijo de tokens que se recargan periodicamente. Cada email consume un token; si no hay tokens disponibles, el envio espera hasta que se recarguen (nunca falla, solo espera).

| Propiedad | Env Variable | Valor por defecto | Descripcion |
|-----------|-------------|-------------------|-------------|
| `notification-service.mail.rate-limit.tokens-per-interval` | `MAIL_RATE_LIMIT_TOKENS` | `2` | Numero maximo de emails que se pueden enviar por intervalo. Con el valor por defecto se permiten 2 emails por segundo |
| `notification-service.mail.rate-limit.refill-interval-ms` | `MAIL_RATE_LIMIT_REFILL_MS` | `1000` | Intervalo en ms para recargar los tokens. Cada vez que pasa este tiempo, los tokens se recargan al maximo |

**Ejemplo de calculo:**
- `tokens-per-interval=2` + `refill-interval-ms=1000` = **2 emails/segundo** = **120 emails/minuto**
- `tokens-per-interval=5` + `refill-interval-ms=1000` = **5 emails/segundo** = **300 emails/minuto**
- `tokens-per-interval=1` + `refill-interval-ms=2000` = **1 email cada 2 segundos** = **30 emails/minuto**

### Email - Reintentos (Exponential Backoff with Jitter)

Cuando un envio de email falla por error transitorio del servidor SMTP, se reintenta hasta **3 veces** usando la estrategia **Exponential Backoff with Jitter**:

| Intento | Delay base | Formula | Rango aproximado |
|---------|-----------|---------|-------------------|
| 1 | 1s | `1000 * 2^0 + random(0, 1000)` | 1s - 2s |
| 2 | 2s | `1000 * 2^1 + random(0, 1000)` | 2s - 3s |
| 3 | - | Ultimo intento, si falla se marca como `FAILED` | - |

El **jitter** (componente aleatorio) evita el efecto *thundering herd*: cuando multiples emails fallan al mismo tiempo, los reintentos se distribuyen de forma aleatoria en vez de ejecutarse todos a la vez.

### Kafka - Configuracion del Cluster

| Propiedad | Valor por defecto | Descripcion |
|-----------|-------------------|-------------|
| `kafka-config.bootstrap-servers` | `localhost:19092, localhost:29092, localhost:39092` | Lista de brokers del cluster de Kafka |
| `kafka-config.schema-registry-url` | `http://localhost:8081` | URL del Schema Registry para schemas Avro |
| `kafka-config.num-of-partitions` | `3` | Numero de particiones al crear topics |
| `kafka-config.replication-factor` | `3` | Factor de replicacion de los topics |

### Kafka - Producer

| Propiedad | Valor por defecto | Descripcion |
|-----------|-------------------|-------------|
| `kafka-producer-config.compression-type` | `snappy` | Algoritmo de compresion de mensajes (snappy ofrece buen balance velocidad/compresion) |
| `kafka-producer-config.acks` | `all` | Nivel de confirmacion: `all` espera que todos los replicas confirmen, garantiza durabilidad maxima |
| `kafka-producer-config.batch-size` | `16384` | Tamano en bytes del batch de mensajes antes de enviar al broker |
| `kafka-producer-config.batch-size-boost-factor` | `100` | Multiplicador del batch-size para optimizar throughput |
| `kafka-producer-config.linger-ms` | `5` | Tiempo en ms que el producer espera antes de enviar un batch incompleto |
| `kafka-producer-config.request-timeout-ms` | `60000` | Timeout en ms para esperar respuesta del broker |
| `kafka-producer-config.retry-count` | `5` | Numero de reintentos si falla el envio al broker |

### Kafka - Consumer

| Propiedad | Valor por defecto | Descripcion |
|-----------|-------------------|-------------|
| `kafka-consumer-config.notification-consumer-group-id` | `notification-topic-consumer` | ID del grupo de consumidores. Todos los consumers con el mismo group-id comparten la carga de mensajes |
| `kafka-consumer-config.auto-offset-reset` | `earliest` | Desde donde leer si no hay offset guardado: `earliest` lee desde el inicio, `latest` solo mensajes nuevos |
| `kafka-consumer-config.specific-avro-reader` | `true` | Deserializa directamente a clases Avro generadas en vez de GenericRecord |
| `kafka-consumer-config.batch-listener` | `true` | Recibe mensajes en lotes (`List<>`) en vez de uno por uno, mejora throughput |
| `kafka-consumer-config.auto-startup` | `true` | El consumer arranca automaticamente con la aplicacion |
| `kafka-consumer-config.concurrency-level` | `3` | Numero de hilos consumidores. Cada hilo procesa particiones de forma independiente. Maximo util = numero de particiones |
| `kafka-consumer-config.session-timeout-ms` | `10000` | Tiempo en ms sin heartbeat antes de que el broker considere al consumer muerto y reasigne sus particiones |
| `kafka-consumer-config.heartbeat-interval-ms` | `3000` | Intervalo en ms entre heartbeats al broker. Debe ser menor que `session-timeout-ms` (recomendado: 1/3) |
| `kafka-consumer-config.max-poll-interval-ms` | `300000` | Tiempo maximo en ms entre llamadas a `poll()`. Si se excede, el consumer es expulsado del grupo. Debe ser suficiente para procesar `max-poll-records` mensajes |
| `kafka-consumer-config.max-poll-records` | `100` | Numero maximo de registros retornados en cada `poll()`. Controla cuantos mensajes se procesan por ciclo |
| `kafka-consumer-config.max-partition-fetch-bytes-default` | `1048576` | Tamano maximo en bytes que el consumer obtiene por particion en cada fetch (1 MB) |
| `kafka-consumer-config.max-partition-fetch-bytes-boost-factor` | `1` | Multiplicador del fetch-bytes por particion |
| `kafka-consumer-config.poll-timeout-ms` | `150` | Tiempo en ms que `poll()` espera si no hay mensajes disponibles antes de retornar vacio |

### Base de Datos

| Propiedad | Valor por defecto | Descripcion |
|-----------|-------------------|-------------|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5434/postgres?currentSchema=notification` | URL de conexion a PostgreSQL, schema `notification` |
| `spring.datasource.username` | `postgres` | Usuario de la base de datos |
| `spring.datasource.password` | `admin` | Password de la base de datos |

## Arquitectura de Resiliencia

```
                    Kafka Consumer (3 hilos, 100 msgs/poll)
                              |
                              v
                    +--------------------+
                    | NotificationHelper |  (persiste en DB + outbox)
                    +--------------------+
                              |
                              v
                    +-------------------------+
                    | NotificationDomainService|  (valida + orquesta)
                    +-------------------------+
                              |
                              v
                    +------------------------+
                    | EmailNotificationSender |
                    |  +------------------+  |
                    |  | Token Bucket     |  |  <-- Controla velocidad (2 emails/s)
                    |  | Rate Limiter     |  |      Si no hay tokens, ESPERA
                    |  +------------------+  |
                    |  +------------------+  |
                    |  | Exponential      |  |  <-- Si SMTP falla, reintenta
                    |  | Backoff + Jitter |  |      con delays crecientes
                    |  +------------------+  |
                    +------------------------+
                              |
                              v
                        SMTP Server
                       (Gmail, etc.)
```
