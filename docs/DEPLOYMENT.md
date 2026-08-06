# Guía de Despliegue Cloud-Agnostic

El proyecto está preparado para desplegarse en **cualquier nube** (Azure, AWS, GCP u on-premise) siguiendo los principios 12-factor:

- **Toda la configuración específica del entorno se inyecta por variables de entorno** (con defaults locales, así el desarrollo local no cambia).
- **Las 4 imágenes Docker son autocontenidas** y no asumen ningún proveedor: JVM consciente del contenedor, usuario no-root, `HEALTHCHECK` integrado y sin perfil de Spring hardcodeado.
- **Health checks estándar** vía Spring Boot Actuator (`/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`) — los que consume cualquier orquestador (Kubernetes, ECS, Container Apps, Cloud Run).
- **Kafka gestionado soportado**: SASL/SSL opcional por variables de entorno (Confluent Cloud, AWS MSK, Azure Event Hubs).
- **Manifiestos de Kubernetes** en `document-notification-system/k8s/` (con kustomize para apuntar al registry real).
- **CI genérico** en `.github/workflows/docker-publish.yml` que publica a cualquier registry (GHCR por defecto; ACR/ECR/Artifact Registry vía secrets).

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
| `SQL_INIT_MODE` | `always` | `never` en la nube (los schemas se crean una sola vez) |

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

### Correo (solo notification-service)

`MAIL_FROM`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_*`, `MAIL_RATE_LIMIT_TOKENS`, `MAIL_RATE_LIMIT_REFILL_MS`, `NOTIFICATION_INSTANCE_ID` (en K8s se inyecta el nombre del pod automáticamente).

## Ejecución local (sin cambios)

```bash
cd document-notification-system
cp .env.example .env   # ajustar credenciales de correo
docker compose up --build
```

## Despliegue en Kubernetes (AKS / EKS / GKE / on-prem)

1. Publicar las imágenes (el workflow `docker-publish.yml` lo hace en cada push a `master`), o manualmente:
   ```bash
   docker build -t <registry>/document-service:1.0 -f document-service/Dockerfile .
   docker push <registry>/document-service:1.0
   # repetir para customer, generator y notification
   ```
2. Aprovisionar los servicios gestionados: PostgreSQL (RDS / Cloud SQL / Azure Database) y Kafka + Schema Registry (MSK / Confluent Cloud / Event Hubs). Crear los schemas (`document`, `generator`, `notification`, `customer`) y los topics una sola vez.
3. Ajustar `k8s/configmap.yaml` con los endpoints reales y crear el secret:
   ```bash
   kubectl apply -f k8s/namespace.yaml
   kubectl -n document-notification-system create secret generic dns-secrets \
     --from-literal=POSTGRES_USER=... --from-literal=POSTGRES_PASSWORD=... \
     --from-literal=MAIL_USERNAME=... --from-literal=MAIL_PASSWORD=...
   ```
4. Apuntar las imágenes al registry real y desplegar:
   ```bash
   cd k8s
   kustomize edit set image document-notification-system/document-service=<registry>/document-service:1.0
   # ... resto de servicios
   kubectl apply -k .
   ```

Los deployments ya incluyen probes de liveness/readiness contra Actuator, límites de recursos y `NOTIFICATION_INSTANCE_ID` único por pod. Para escalar: `kubectl scale deployment notification-service --replicas=3` (los consumer groups de Kafka y el locking optimista del outbox soportan múltiples instancias).

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
