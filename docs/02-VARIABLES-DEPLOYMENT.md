# Variables de Despliegue

Referencia única de **todas las variables de entorno** del Document Notification System: los defaults locales, las plantillas `.env` del repo, y las tablas específicas del despliegue en Azure. El proyecto está preparado para desplegarse en **cualquier nube** (Azure, AWS, GCP u on-premise) siguiendo los principios 12-factor:

- **Toda la configuración específica del entorno se inyecta por variables de entorno** (con defaults locales, así el desarrollo local no cambia).
- **Las 4 imágenes Docker son autocontenidas** y no asumen ningún proveedor: JVM consciente del contenedor, usuario no-root, `HEALTHCHECK` integrado y sin perfil de Spring hardcodeado.
- **Health checks estándar** vía Spring Boot Actuator (`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`) — los que consume cualquier orquestador (Kubernetes, ECS, Container Apps, Cloud Run).
- **Kafka gestionado soportado**: SASL/SSL opcional por variables de entorno (Confluent Cloud, AWS MSK, Azure Event Hubs).
- **CI genérico** en `.github/workflows/docker-publish.yml` que publica a cualquier registry (GHCR por defecto; ACR/ECR/Artifact Registry vía secrets).

## Las plantillas `.env` del repo

Las variables de este documento se materializan en dos plantillas versionadas — son el punto de partida de cualquier despliegue:

| Plantilla | Entorno | Cómo se usa |
|---|---|---|
| [`document-notification-system/.env.example`](../document-notification-system/.env.example) | **Local (docker-compose)** | `cp .env.example .env` y ajustar credenciales de correo; docker-compose la lee automáticamente |
| [`document-notification-system/.env-cloud`](../document-notification-system/.env-cloud) | **Azure (Container Apps)** | Es la **hoja de trabajo** del despliegue: se llena a medida que se avanza por la [guía paso a paso](03-AZURE-ESTUDIANTE-PASO-A-PASO.md) y se carga en la terminal (`set -a; source .env-cloud; set +a`) para que los comandos `az` la consuman — ver la [sección 7.5 de la guía](03-AZURE-ESTUDIANTE-PASO-A-PASO.md#75-cargar-env-cloud-en-la-terminal-y-desplegar-sin-copiarpegar) |

⚠️ Ambas plantillas se versionan **con valores vacíos**. Llenas con credenciales reales, **no se comitean**.

## Variables de entorno

### Comunes a los 4 servicios

| Variable | Default | Descripción |
|---|---|---|
| `SERVER_PORT` | 8181/8182/8183/8184 | Puerto HTTP del servicio |
| `SPRING_PROFILES_ACTIVE` | *(vacío)* | `docker` para docker-compose; en la nube normalmente se deja vacío y se configura todo por env vars |
| `APP_LOG_LEVEL` | `DEBUG` (local) / `INFO` (docker) | Nivel de log de la aplicación |
| `JPA_SHOW_SQL` | `true` (local) / `false` (docker) | Log de SQL |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0` | Opciones de la JVM |

### Base de datos (PostgreSQL)

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` (base) / `postgres` (docker) | Host de PostgreSQL (RDS, Cloud SQL, Azure Database…) |
| `DB_PORT` | `5434` (base) / `5432` (docker) | Puerto |
| `DB_NAME` | `postgres` | Base de datos (cada servicio usa su propio schema: `document`, `generator`, `notification`, `customer`) |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | `postgres` / `admin` | Credenciales |
| `DB_EXTRA_PARAMS` | *(vacío)* | Parámetros JDBC extra, ej. `&sslmode=require` para Postgres gestionado |
| `SQL_INIT_MODE` | `never` | Default seguro: la app **no** ejecuta `init-schema.sql` / `init-data.sql` al arrancar. El esquema se crea una sola vez con `infraestructure/docker-compose/init-db.sql` — el contenedor de Postgres lo corre solo en local, y en la nube se ejecuta con `psql` (paso 5 de la guía). **No lo pongas en `always`**: el `DROP SCHEMA customer CASCADE` de ese script destruye la vista materializada `"document".customers` |

### Kafka / Schema Registry

| Variable | Default | Descripción |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | brokers locales | Bootstrap servers del cluster |
| `SCHEMA_REGISTRY_URL` | `http://localhost:8081` | URL del Schema Registry |
| `KAFKA_REPLICATION_FACTOR` | `3` | Factor de replicación de los topics |
| `KAFKA_NUM_PARTITIONS` | `3` | Particiones por topic |
| `KAFKA_CONSUMER_CONCURRENCY` | `3` | Hilos por listener |
| `KAFKA_SECURITY_PROTOCOL` | *(vacío = PLAINTEXT)* | `SASL_SSL` para Kafka gestionado |
| `KAFKA_SASL_MECHANISM` | *(vacío)* | `PLAIN` (Confluent/Event Hubs) o `AWS_MSK_IAM` |
| `KAFKA_SASL_JAAS_CONFIG` | *(vacío)* | Cadena JAAS con credenciales |
| `SCHEMA_REGISTRY_AUTH_USER_INFO` | *(vacío)* | `key:secret` para Schema Registry con auth básica |
| `KAFKA_PRODUCER_COMPRESSION_TYPE` | `none` | Compresión del productor (`generator` y `notification`). `snappy` requiere binarios nativos que **no cargan en la imagen Alpine** — dejar `none` salvo que se cambie la imagen base |

### Correo (solo notification-service)

| Variable | Default | Descripción |
|---|---|---|
| `MAIL_PROVIDER` | `azure` | Proveedor de envío: `azure` (Azure Communication Services Email, para envío masivo — el default) o `smtp` (Gmail o cualquier SMTP; es lo que fuerza `docker-compose` en local) |
| `MAIL_FROM` | `no-reply@example.com` | Remitente. Con `azure` debe ser la dirección verificada de ACS (`donotreply@<guid>.azurecomm.net`) |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_*` | Gmail/587 | Configuración SMTP — solo aplican con `MAIL_PROVIDER=smtp` |
| `ACS_CONNECTION_STRING` | *(vacío)* | Connection string de Azure Communication Services — **obligatorio** con el proveedor `azure` (falla al arrancar con mensaje claro si falta) |
| `ACS_EMAIL_TIMEOUT_SECONDS` | `60` | Espera máxima por la confirmación de envío de ACS |
| `MAIL_RATE_LIMIT_TOKENS`, `MAIL_RATE_LIMIT_REFILL_MS` | `10` / `3600000` | Rate limiter (Token Bucket), aplica a ambos proveedores. El default = 10 correos/hora, la cuota de ACS sobre Azure Managed Domain (5/min y 10/hora, **no ampliables**). `docker-compose` lo relaja a `100`/`1000` para local, donde el proveedor es SMTP y no hay cuota |
| `NOTIFICATION_INSTANCE_ID` | `notification-1` | Único por instancia (en K8s se inyecta el nombre del pod automáticamente) |

## Variables para el despliegue en Azure (guía paso a paso)

Las dos tablas de la [guía Azure paso a paso](03-AZURE-ESTUDIANTE-PASO-A-PASO.md), consolidadas aquí. Corresponden a los dos bloques de la plantilla `.env-cloud`.

### Nombres de recursos (solo para los comandos `az`)

El primer bloque de `.env-cloud` no lo leen los microservicios: son los **nombres que tú les diste a los recursos de Azure** al crearlos. Existen para parametrizar los comandos `az` y para que no tengas que recordar qué nombre usaste en cada paso. Dos de ellas, además, **derivan valores que sí usa la aplicación**:

| Variable | Valor en la guía | Qué nombra y por qué importa |
|---|---|---|
| `RESOURCE_GROUP` | `dns-student-rg` | El grupo de recursos (paso 3.1) — la "carpeta" que agrupa todo. Es el `-g`/`--resource-group` de **todos** los comandos `az`, y lo que borras al final del semestre con `az group delete` |
| `LOCATION` | `eastus` | La región de los Container Apps y el environment. (La BD puede vivir en otra — en el despliegue real quedó en `centralus`, ver paso 5b de la guía) |
| `ACR_NAME` | `dnsstudentacr` | El **Azure Container Registry** (paso 3.2). Debe ser **único en todo Azure** (solo minúsculas/números) porque forma el DNS del registro: `<ACR_NAME>.azurecr.io`. De él derivan las etiquetas de las imágenes (`docker tag ... <ACR_NAME>.azurecr.io/document-service:1.0`) y los flags `--image` y `--registry-server` del paso 8 |
| `CONTAINERAPPS_ENV` | `dns-student-env` | El **Container Apps Environment** (paso 8): la red privada compartida donde viven las 4 apps (y donde `generator`/`notification` quedan escondidos con ingress `internal`). Es el valor de `--environment` en cada `az containerapp create` — las 4 apps deben apuntar **al mismo** para poder verse entre sí |
| `PG_SERVER_NAME` | `dns-student-pg` | El **PostgreSQL Flexible Server** (paso 5). También único globalmente, porque forma el DNS del servidor: `<PG_SERVER_NAME>.postgres.database.azure.com` — y ese DNS es exactamente el valor de `DB_HOST` que sí leen los 4 servicios. Es el `-n` de los comandos de operación de la BD (`stop`/`start`/`update`) |
| `ACS_RESOURCE_NAME` / `ACS_EMAIL_SERVICE_NAME` | `dns-comm` / `dns-email` | Los recursos de correo (paso 7: recurso de comunicación y servicio de email). Se usan en los comandos de `az communication` para obtener la connection string y el dominio remitente |

En resumen: `RESOURCE_GROUP`, `LOCATION`, `CONTAINERAPPS_ENV` y los nombres de ACS solo viven en los comandos; `ACR_NAME` y `PG_SERVER_NAME` además determinan valores de la aplicación (el prefijo de las imágenes y `DB_HOST` respectivamente) — si los cambias, cambia también lo que despliegas.

### Variables de la aplicación en Azure

Lo que **SÍ o SÍ debes configurar en Azure**, agrupado por categoría:

| Categoría | Variable | Valor en Azure | Notas |
|---|---|---|---|
| **Base de datos** | `DB_HOST` | `<server>.postgres.database.azure.com` | Host del Flexible Server |
| | `DB_PORT` / `DB_NAME` | `5432` / `postgres` | |
| | `POSTGRES_USER` / `POSTGRES_PASSWORD` | admin del paso 5 / `secretref:pgpass` | Contraseña **siempre** como secreto |
| | `DB_EXTRA_PARAMS` | `&sslmode=require` | Azure solo acepta conexiones cifradas |
| | `SQL_INIT_MODE` | `never` (siempre) | El esquema y los datos semilla los crea `init-db.sql` con `psql` en el paso 5, incluidas `customer.customers` y la vista `"document".customers`. Nunca `always`: destruye esa vista y con réplicas > 1 además provoca carreras |
| **Kafka** | `KAFKA_BOOTSTRAP_SERVERS` | `pkc-xxxxx...confluent.cloud:9092` | Del paso 6 |
| | `KAFKA_SECURITY_PROTOCOL` | `SASL_SSL` | Kafka gestionado siempre cifrado |
| | `KAFKA_SASL_MECHANISM` | `PLAIN` | |
| | `KAFKA_SASL_JAAS_CONFIG` | `secretref:kafkajaas` | Cadena JAAS con API key/secret del cluster |
| **Schema Registry** | `SCHEMA_REGISTRY_URL` | `https://psrc-xxxxx...confluent.cloud` | |
| | `SCHEMA_REGISTRY_AUTH_USER_INFO` | `secretref:srauth` (`key:secret`) | |
| **Correo** (solo `notification-service`) | `MAIL_PROVIDER` | `azure` (default, puede omitirse) | `smtp` solo para Gmail/Mailpit |
| | `ACS_CONNECTION_STRING` | `secretref:acsconn` | **Obligatoria** con el proveedor `azure` |
| | `MAIL_FROM` | `donotreply@<guid>.azurecomm.net` | El dominio verificado de ACS del paso 7 |
| | `MAIL_RATE_LIMIT_TOKENS` / `MAIL_RATE_LIMIT_REFILL_MS` | `10` / `3600000` | = 10/hora, la cuota del Azure Managed Domain. Subirlo provoca 429. Con [dominio propio verificado](03-AZURE-ESTUDIANTE-PASO-A-PASO.md#76-opcional-dominio-propio-subir-la-cuota-de-10hora-a-100hora): `100`/`3600000` |
| **Operación** | `APP_LOG_LEVEL` | `INFO` (`WARN` en pruebas de carga) | |
| | `JPA_SHOW_SQL` | `false` | En `true` imprime cada SQL: lento y ruidoso |
| **Escalado** | `NOTIFICATION_INSTANCE_ID` | único por instancia | Solo si creas varias *apps* de notification (ver escalado en la guía); el outbox lo usa para locking |
| | `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE` | `5` | Conexiones por réplica. El B1ms trae `max_connections = 50`, así que `Σ (réplicas × pool) + ~5 reservadas` debe caber ahí. Ojo: un rolling restart duplica la demanda momentáneamente |

Reglas transversales (aplican a los 4 servicios):

- **Credenciales siempre como secretos** (`--secrets` + `secretref:`), nunca en texto plano en `--env-vars`.
- **TLS en todo**: `sslmode=require` a la BD, `SASL_SSL` a Kafka, HTTPS a ACS.
- `SPRING_PROFILES_ACTIVE` se deja **vacío** en la nube: toda la configuración entra por variables (el perfil `docker` es solo para docker-compose local).

## Ejecución local (sin cambios)

```bash
cd document-notification-system
cp .env.example .env   # ajustar credenciales de correo
docker compose up --build
```

## Despliegue en Kubernetes (AKS / EKS / GKE / on-prem)

Las imágenes funcionan en cualquier cluster sin cambios; solo hay que inyectar las variables de este documento:

1. Publicar las imágenes (el workflow `docker-publish.yml` lo hace en cada push a `master`), o manualmente:
   ```bash
   docker build -t <registry>/document-service:1.0 -f document-service/Dockerfile .
   docker push <registry>/document-service:1.0
   # repetir para customer, generator y notification
   ```
2. Aprovisionar los servicios gestionados: PostgreSQL (RDS / Cloud SQL / Azure Database) y Kafka + Schema Registry (MSK / Confluent Cloud / Event Hubs). Crear los schemas (`document`, `generator`, `notification`, `customer`) y los topics una sola vez.
3. En los deployments: las variables no sensibles van en un ConfigMap y las credenciales (BD, JAAS de Kafka, Schema Registry, `ACS_CONNECTION_STRING`) en un Secret; probes de liveness/readiness contra `/actuator/health/liveness` y `/actuator/health/readiness`; y `NOTIFICATION_INSTANCE_ID` único por pod (ej. inyectando `metadata.name` vía fieldRef).

Para escalar: `kubectl scale deployment notification-service --replicas=3` (los consumer groups de Kafka y el locking optimista del outbox soportan múltiples instancias; máximo 3 réplicas útiles por las 3 particiones).

## Servicios de contenedores gestionados

Las mismas imágenes y variables funcionan sin cambios en:

- **Azure Container Apps / App Service**: configurar env vars en la app; health probe → `/actuator/health/readiness`. Kafka: Event Hubs con `KAFKA_SECURITY_PROTOCOL=SASL_SSL`, `KAFKA_SASL_MECHANISM=PLAIN` y el connection string en el JAAS config.
- **AWS ECS / App Runner**: env vars en la task definition (secrets desde Secrets Manager); el `HEALTHCHECK` del Dockerfile lo usa ECS directamente. Kafka: MSK.
- **GCP Cloud Run / GKE**: env vars + Secret Manager; startup/liveness probes contra Actuator.

### Ejemplo: Kafka gestionado (Confluent Cloud)

```
KAFKA_BOOTSTRAP_SERVERS=pkc-xxxxx.region.provider.confluent.cloud:9092
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_SASL_JAAS_CONFIG=org.apache.kafka.common.security.plain.PlainLoginModule required username="<API_KEY>" password="<API_SECRET>";
SCHEMA_REGISTRY_URL=https://psrc-xxxxx.region.provider.confluent.cloud
SCHEMA_REGISTRY_AUTH_USER_INFO=<SR_KEY>:<SR_SECRET>
```

### Ejemplo: Postgres gestionado

```
DB_HOST=mydb.xxxxx.us-east-1.rds.amazonaws.com
DB_PORT=5432
DB_NAME=postgres
DB_EXTRA_PARAMS=&sslmode=require
SQL_INIT_MODE=never
```

## Checklist para producción

- [ ] `SQL_INIT_MODE=never` (inicializar schemas una única vez, fuera del arranque).
- [ ] `APP_LOG_LEVEL=INFO` y `JPA_SHOW_SQL=false`.
- [ ] Credenciales en el gestor de secretos del proveedor, nunca en los manifiestos.
- [ ] `KAFKA_REPLICATION_FACTOR` acorde al cluster (Confluent Cloud básico = 3; un solo broker = 1).
- [ ] TLS a Postgres (`DB_EXTRA_PARAMS=&sslmode=require`) y SASL_SSL a Kafka.
- [ ] Escalar `notification-service` y `generator-service` según carga; los topics tienen 3 particiones (máximo 3 consumidores activos por grupo).
