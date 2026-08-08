# Despliegue en Azure — Paso a Paso

Guía para desplegar el Document Notification System en **Azure Container Apps**, usando Azure Container Registry, Azure Database for PostgreSQL y Confluent Cloud (Kafka + Schema Registry) desde el Azure Marketplace.

> Prerrequisitos: Azure CLI (`az login`), una suscripción activa. Docker local es opcional (las imágenes se construyen en la nube con `az acr build`).

## Arquitectura del despliegue

| Componente | Servicio de Azure |
|---|---|
| Imágenes Docker (x4) | Azure Container Registry (ACR) |
| Microservicios (x4) | Azure Container Apps |
| PostgreSQL | Azure Database for PostgreSQL Flexible Server |
| Kafka + Schema Registry | Confluent Cloud (Azure Marketplace) |
| Secretos | Secrets de Container Apps (`secretref:`) |

## Paso 1 — Grupo de recursos y registry (ACR)

```bash
az group create -n dns-rg -l eastus
az acr create -g dns-rg -n dnsregistro --sku Basic --admin-enabled true
```

## Paso 2 — Construir y subir las 4 imágenes

`az acr build` compila en la nube (no requiere Docker local):

```bash
cd document-notification-system
az acr build -r dnsregistro -t document-service:1.0     -f document-service/Dockerfile .
az acr build -r dnsregistro -t customer-service:1.0     -f customer-service/Dockerfile .
az acr build -r dnsregistro -t generator-service:1.0    -f generator-service/Dockerfile .
az acr build -r dnsregistro -t notification-service:1.0 -f notification-service/Dockerfile .
```

> Alternativa CI: configurar en GitHub los secrets `REGISTRY_URL`, `REGISTRY_USERNAME`, `REGISTRY_PASSWORD` (credenciales del ACR) y el workflow `.github/workflows/docker-publish.yml` publica las imágenes en cada push a `master`.

## Paso 3 — PostgreSQL gestionado

```bash
az postgres flexible-server create -g dns-rg -n dns-postgres \
  --admin-user dnsadmin --admin-password '<PASSWORD-FUERTE>' \
  --sku-name Standard_B1ms --tier Burstable --version 15 \
  --public-access 0.0.0.0   # permite acceso desde servicios de Azure
```

Crear los schemas **una sola vez** (mismo script que usa docker-compose):

```bash
psql "host=dns-postgres.postgres.database.azure.com port=5432 dbname=postgres user=dnsadmin sslmode=require" \
  -f infraestructure/docker-compose/init-db.sql
```

## Paso 4 — Kafka + Schema Registry (Confluent Cloud)

El sistema usa Avro con Confluent Schema Registry, así que lo más directo es Confluent Cloud:

1. Portal de Azure → **Marketplace → Apache Kafka on Confluent Cloud** → crear organización y un cluster *Basic* en la misma región (eastus).
2. En Confluent, crear los 5 topics con **3 particiones**: `customer`, `generator-request`, `generator-response`, `notification-request`, `notification-response`.
3. Habilitar **Schema Registry** (Essentials) en la misma región.
4. Generar credenciales: un **API key/secret del cluster** y otro **del Schema Registry**.

Guardar estos 4 valores:
- Bootstrap server: `pkc-xxxxx.eastus.azure.confluent.cloud:9092`
- API key/secret del cluster
- URL del Schema Registry: `https://psrc-xxxxx.eastus.azure.confluent.cloud`
- Key/secret del Schema Registry

> Alternativa: Azure Event Hubs tiene endpoint compatible con Kafka (mismo `SASL_SSL`/`PLAIN` con el connection string), pero su Schema Registry **no** es compatible con el serializador de Confluent — habría que desplegar un contenedor `cp-schema-registry` propio.

## Paso 5 — Entorno de Container Apps

```bash
az containerapp env create -g dns-rg -n dns-env -l eastus
```

## Paso 6 — Desplegar los servicios

Desplegar primero `customer-service`, luego el resto. Ejemplo completo para uno — los demás son el mismo comando cambiando nombre, imagen y puerto:

```bash
az containerapp create -g dns-rg -n customer-service \
  --environment dns-env \
  --image dnsregistro.azurecr.io/customer-service:1.0 \
  --registry-server dnsregistro.azurecr.io \
  --target-port 8184 --ingress external \
  --min-replicas 1 --max-replicas 1 \
  --secrets pgpass='<PASSWORD-FUERTE>' \
            kafkajaas='org.apache.kafka.common.security.plain.PlainLoginModule required username="<API_KEY>" password="<API_SECRET>";' \
            srauth='<SR_KEY>:<SR_SECRET>' \
  --env-vars \
    DB_HOST=dns-postgres.postgres.database.azure.com \
    DB_PORT=5432 DB_NAME=postgres \
    'DB_EXTRA_PARAMS=&sslmode=require' \
    POSTGRES_USER=dnsadmin POSTGRES_PASSWORD=secretref:pgpass \
    SQL_INIT_MODE=never \
    KAFKA_BOOTSTRAP_SERVERS='pkc-xxxxx.eastus.azure.confluent.cloud:9092' \
    KAFKA_SECURITY_PROTOCOL=SASL_SSL \
    KAFKA_SASL_MECHANISM=PLAIN \
    KAFKA_SASL_JAAS_CONFIG=secretref:kafkajaas \
    SCHEMA_REGISTRY_URL='https://psrc-xxxxx.eastus.azure.confluent.cloud' \
    SCHEMA_REGISTRY_AUTH_USER_INFO=secretref:srauth \
    APP_LOG_LEVEL=INFO
```

Diferencias por servicio:

| Servicio | `--target-port` | `--ingress` | Extras |
|---|---|---|---|
| `customer-service` | 8184 | `external` | En el **primer** arranque usar `SQL_INIT_MODE=always` (carga `init-schema.sql` + `init-data.sql`); luego cambiarlo a `never` |
| `document-service` | 8181 | `external` (API principal) | — |
| `generator-service` | 8182 | `internal` | — |
| `notification-service` | 8183 | `internal` | **Por defecto** envía con Azure Communication Services (`MAIL_PROVIDER=azure`): secret `acsconn` + env vars `ACS_CONNECTION_STRING=secretref:acsconn` y `MAIL_FROM=donotreply@<guid>.azurecomm.net` — ver [`AZURE-ESCALADO-PRUEBAS-MASIVAS.md`](AZURE-ESCALADO-PRUEBAS-MASIVAS.md) sección 0.2. Alternativa Gmail/SMTP: `MAIL_PROVIDER=smtp` + secret `mailpass` + env vars `MAIL_FROM`, `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME`, `MAIL_PASSWORD=secretref:mailpass` |

> `NOTIFICATION_INSTANCE_ID` no hace falta fijarlo: si se omite usa el default, y al escalar conviene ponerlo distinto por réplica (en AKS se inyecta el nombre del pod automáticamente).

## Paso 7 — Health probes

En el portal (Container App → *Containers* → *Health probes*) o vía YAML:

- **Readiness:** HTTP GET `/actuator/health/readiness` en el puerto del servicio.
- **Liveness:** HTTP GET `/actuator/health/liveness`.

## Paso 8 — Verificar

```bash
# URL pública del API
az containerapp show -g dns-rg -n document-service \
  --query properties.configuration.ingress.fqdn -o tsv
curl https://<fqdn>/actuator/health

# Logs en vivo
az containerapp logs show -g dns-rg -n notification-service --follow
```

## Paso 9 — Escalar

```bash
az containerapp update -g dns-rg -n notification-service --min-replicas 2 --max-replicas 3
az containerapp update -g dns-rg -n generator-service    --min-replicas 2 --max-replicas 3
```

Máximo 3 réplicas activas consumiendo por servicio (los topics tienen 3 particiones). El outbox con locking optimista ya tolera múltiples instancias.

## Alternativa: AKS (Kubernetes)

Los pasos 1–4 son idénticos. Después:

```bash
az aks create -g dns-rg -n dns-aks --node-count 2 --attach-acr dnsregistro
az aks get-credentials -g dns-rg -n dns-aks

# Ajustar k8s/configmap.yaml con los endpoints reales, luego:
kubectl apply -f document-notification-system/k8s/namespace.yaml
kubectl -n document-notification-system create secret generic dns-secrets \
  --from-literal=POSTGRES_USER=dnsadmin \
  --from-literal=POSTGRES_PASSWORD='<PASSWORD-FUERTE>' \
  --from-literal=MAIL_USERNAME=... --from-literal=MAIL_PASSWORD=... \
  --from-literal=KAFKA_SECURITY_PROTOCOL=SASL_SSL \
  --from-literal=KAFKA_SASL_MECHANISM=PLAIN \
  --from-literal=KAFKA_SASL_JAAS_CONFIG='...' \
  --from-literal=SCHEMA_REGISTRY_AUTH_USER_INFO='<SR_KEY>:<SR_SECRET>'

cd document-notification-system/k8s
kustomize edit set image document-notification-system/document-service=dnsregistro.azurecr.io/document-service:1.0
# ... repetir para los otros 3 servicios
kubectl apply -k .
```

Los manifiestos ya incluyen probes de liveness/readiness, límites de recursos e instance-id único por pod para `notification-service`.

## Checklist final

- [ ] `SQL_INIT_MODE=never` en todos los servicios tras la primera inicialización.
- [ ] `APP_LOG_LEVEL=INFO`, `JPA_SHOW_SQL=false`.
- [ ] Contraseñas siempre como secrets (`secretref:` / K8s Secret), nunca en texto plano.
- [ ] TLS: `DB_EXTRA_PARAMS=&sslmode=require` y `KAFKA_SECURITY_PROTOCOL=SASL_SSL`.
- [ ] Topics creados con 3 particiones y factor de replicación 3.
