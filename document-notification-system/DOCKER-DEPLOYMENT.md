# Document Notification System - Deployment Guide

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Part 1: Local Docker Deployment](#part-1-local-docker-deployment)
  - [Quick Start](#quick-start)
  - [Running Infrastructure Only](#running-infrastructure-only)
  - [Running Individual Services](#running-individual-services)
  - [Scaling Local Instances](#scaling-local-instances)
  - [Useful Commands](#useful-commands)
  - [Troubleshooting Local](#troubleshooting-local)
- [Part 2: Azure Multi-Instance Deployment](#part-2-azure-multi-instance-deployment)
  - [Azure Architecture](#azure-architecture)
  - [Step 1 - Azure Prerequisites](#step-1---azure-prerequisites)
  - [Step 2 - Create Azure Resources](#step-2---create-azure-resources)
  - [Step 3 - Build and Push Images](#step-3---build-and-push-images)
  - [Step 4 - Deploy Microservices](#step-4---deploy-microservices)
  - [Step 5 - Configure Scaling Rules](#step-5---configure-scaling-rules)
  - [Step 6 - Configure Kafka](#step-6---configure-kafka)
  - [Monitoring and Logs](#monitoring-and-logs)
  - [Tear Down](#tear-down)
- [Environment Variables Reference](#environment-variables-reference)
- [Spring Profiles](#spring-profiles)
- [Project Structure](#project-structure)

---

## Architecture Overview

```
                        ┌──────────────┐
                        │  PostgreSQL   │
                        │  4 schemas:   │
                        │  customer     │
                        │  document     │
                        │  generator    │
                        │  notification │
                        └──────┬───────┘
                               │
           ┌───────────────────┼───────────────────┐
           │                   │                   │
  ┌────────┴───────┐  ┌───────┴───────┐  ┌────────┴───────┐  ┌──────────────┐
  │  Document      │  │  Generator    │  │  Notification  │  │  Customer    │
  │  Service       │  │  Service      │  │  Service       │  │  Service     │
  │  :8181         │  │  :8182        │  │  :8183         │  │  :8184       │
  │  (REST API)    │  │  (worker)     │  │  (worker+SMTP) │  │  (REST API)  │
  └────────┬───────┘  └───────┬───────┘  └────────┬───────┘  └──────┬───────┘
           │                  │                    │                 │
           └──────────────────┴────────────────────┴─────────────────┘
                                      │
                     ┌────────────────┴────────────────┐
                     │         Kafka Cluster            │
                     │  3 brokers + Schema Registry     │
                     │  + Zookeeper                     │
                     │                                  │
                     │  Topics:                         │
                     │  - customer (3 partitions)       │
                     │  - generator-request (3 part.)   │
                     │  - generator-response (3 part.)  │
                     │  - notification-request (3 part.)│
                     │  - notification-response (3 p.)  │
                     └─────────────────────────────────┘
```

**Service Roles:**

| Service | Type | Purpose |
|---------|------|---------|
| `document-service` | REST API + Saga orchestrator | Entry point. Creates documents, triggers generation and notification sagas |
| `generator-service` | Kafka worker | Consumes `generator-request`, generates documents, publishes `generator-response` |
| `notification-service` | Kafka worker + SMTP | Consumes `notification-request`, sends emails, publishes `notification-response` |
| `customer-service` | REST API + Kafka publisher | Manages customers, publishes events to `customer` topic |

**Multi-Instance Safety:**

| Mechanism | What it protects |
|-----------|-----------------|
| Kafka consumer groups | Each instance gets a subset of partitions (automatic rebalancing) |
| Outbox `version` column | Optimistic locking prevents duplicate outbox processing |
| `NOTIFICATION_INSTANCE_ID` | Unique identity per notification-service instance |

---

## Prerequisites

- **Docker Desktop** >= 4.x with Docker Compose v2
- **Java 19** (only for local development without Docker)
- **Maven 3.8+** (only for local development without Docker)
- **Git**
- ~8 GB of free RAM (Kafka cluster + 4 services)

---

## Part 1: Local Docker Deployment

### Quick Start

```bash
# 1. Navigate to the project
cd document-notification-system

# 2. Build and start everything (first run takes ~5-10 min for Maven build)
docker compose up --build -d

# 3. Watch startup progress
docker compose logs -f --tail=50

# 4. Wait until all services show "Started *Application in X seconds"
#    Then test:
curl http://localhost:8184/api/customers
curl http://localhost:8181/api/documents

# 5. Stop everything
docker compose down
```

**What happens during startup:**
1. PostgreSQL starts and executes `init-db.sql` (creates all 4 schemas + seed data)
2. Zookeeper starts, then 3 Kafka brokers connect to it
3. Schema Registry starts and connects to Kafka
4. `init-kafka` creates the 5 required topics, then exits
5. The 4 microservices start with `--spring.profiles.active=docker`

### Running Infrastructure Only

If you want to run the services from your IDE (IntelliJ, VS Code) but need the infrastructure:

```bash
# Start only PostgreSQL + Kafka + Schema Registry
docker compose up postgres zookeeper kafka-broker-1 kafka-broker-2 kafka-broker-3 schema-registry init-kafka -d
```

Then run each service from your IDE with the **default** profile (uses `localhost` connections).

```bash
# Or with Maven directly:
cd document-notification-system
mvn clean install -DskipTests

# Terminal 1 - Customer Service
mvn -pl customer-service/customer-container spring-boot:run

# Terminal 2 - Document Service
mvn -pl document-service/document-container spring-boot:run

# Terminal 3 - Generator Service
mvn -pl generator-service/generator-container spring-boot:run

# Terminal 4 - Notification Service
mvn -pl notification-service/notification-container spring-boot:run
```

### Running Individual Services

```bash
# Build only one service image
docker compose build document-service

# Start one service (infrastructure must be running)
docker compose up document-service -d

# Rebuild and restart one service after code changes
docker compose up --build document-service -d

# View logs of one service
docker compose logs -f notification-service
```

### Scaling Local Instances

There are 3 ways to run multiple instances locally:

#### Option A: `.env` file (recommended for repeatable setups)

Create or edit `.env` in `document-notification-system/`:

```env
# Number of replicas per service
GENERATOR_REPLICAS=2
NOTIFICATION_REPLICAS=3

# Email config (required for notification-service)
MAIL_FROM=your-email@gmail.com
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

```bash
docker compose up --build -d
# Result: 1 customer, 1 document, 2 generator, 3 notification instances
```

#### Option B: `--scale` flag (quick, one-off)

```bash
docker compose up --build -d \
  --scale generator-service=2 \
  --scale notification-service=3
```

#### Option C: Mix of Docker + IDE (for debugging one instance)

```bash
# Start infrastructure + 2 notification instances in Docker
docker compose up --build -d --scale notification-service=2

# Start a 3rd notification instance from IDE with debug breakpoints
# (uses default profile = localhost connections, which works because
#  Docker ports are forwarded to localhost)
```

**Verify all instances are running:**

```bash
docker compose ps
```

Expected output:
```
NAME                                  STATUS
notification-service-1                running
notification-service-2                running
notification-service-3                running
generator-service-1                   running
generator-service-2                   running
document-service-1                    running
customer-service-1                    running
...infrastructure services...
```

**Check Kafka consumer group partition assignment:**

```bash
docker compose exec kafka-broker-1 kafka-consumer-groups \
  --bootstrap-server kafka-broker-1:9092 \
  --describe \
  --group notification-topic-consumer
```

Expected output (3 instances, 3 partitions = 1 partition each):
```
GROUP                        TOPIC                  PARTITION  CURRENT-OFFSET  HOST
notification-topic-consumer  notification-request   0          12              /172.18.0.15
notification-topic-consumer  notification-request   1          8               /172.18.0.16
notification-topic-consumer  notification-request   2          15              /172.18.0.17
```

**Scaling limits:**

| Constraint | Default | Impact |
|-----------|---------|--------|
| Kafka partitions per topic | 3 | Max 3 instances consuming in parallel per consumer group |
| PostgreSQL connections | 100 | ~25 connections per service instance (watch `max_connections`) |
| Host memory | - | Each JVM uses ~256-512 MB |

To increase Kafka partitions (allows more parallel consumers):

```bash
docker compose exec kafka-broker-1 kafka-topics \
  --bootstrap-server kafka-broker-1:9092 \
  --alter \
  --topic notification-request \
  --partitions 6
```

### Useful Commands

```bash
# Restart a specific service without rebuilding
docker compose restart notification-service

# View real-time logs across all services
docker compose logs -f --tail=20

# Check resource usage per container
docker stats

# Access PostgreSQL directly
docker compose exec postgres psql -U postgres -d postgres

# List all Kafka topics
docker compose exec kafka-broker-1 kafka-topics \
  --bootstrap-server kafka-broker-1:9092 --list

# Consume messages from a topic (debug)
docker compose exec kafka-broker-1 kafka-console-consumer \
  --bootstrap-server kafka-broker-1:9092 \
  --topic notification-request \
  --from-beginning --max-messages 5

# Full cleanup (containers + volumes + images)
docker compose down -v --rmi local
```

### Troubleshooting Local

| Problem | Cause | Solution |
|---------|-------|----------|
| `Port 5434 already in use` | Local PostgreSQL running | Stop local PG or change port in `docker-compose.yml` |
| Service exits with code 1 | Kafka not ready yet | Increase `retries` in healthcheck, or just `docker compose up -d` again |
| `OptimisticLockException` in logs | Expected with multiple instances | Not an error. Means another instance already processed that outbox entry |
| `No partitions assigned` | More instances than partitions | Increase partitions (`kafka-topics --alter --partitions N`) |
| `Connection refused to postgres` | DB not ready | Wait for healthcheck. Services have `restart: on-failure` |
| Slow first build (~10 min) | Maven downloading dependencies | Subsequent builds use Docker BuildKit cache |

---

## Part 2: Azure Multi-Instance Deployment

### Azure Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Azure Resource Group                        │
│                  rg-document-notification-system                 │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │          Container Apps Environment                       │   │
│  │          env-document-notification                         │   │
│  │                                                           │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │   │
│  │  │ document-   │  │ generator-  │  │ notification-   │  │   │
│  │  │ service     │  │ service     │  │ service         │  │   │
│  │  │ (external)  │  │ (internal)  │  │ (internal)      │  │   │
│  │  │ 1-3 replicas│  │ 1-5 replicas│  │ 1-10 replicas   │  │   │
│  │  └──────┬──────┘  └──────┬──────┘  └───────┬─────────┘  │   │
│  │         │                │                  │            │   │
│  │  ┌──────┴────────────────┴──────────────────┴─────────┐  │   │
│  │  │           Internal Virtual Network                  │  │   │
│  │  └──────────────────────┬─────────────────────────────┘  │   │
│  │                         │                                 │   │
│  │  ┌─────────────┐       │                                 │   │
│  │  │ customer-   │───────┘                                 │   │
│  │  │ service     │                                         │   │
│  │  │ (internal)  │                                         │   │
│  │  │ 1-3 replicas│                                         │   │
│  │  └─────────────┘                                         │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌──────────────────┐  ┌──────────────────────────────────┐     │
│  │ Azure Container  │  │ Azure Database for PostgreSQL    │     │
│  │ Registry (ACR)   │  │ Flexible Server (Standard_B1ms) │     │
│  │ acrdocnotif      │  │ pgdocnotif                      │     │
│  └──────────────────┘  └──────────────────────────────────┘     │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Kafka (choose one):                                       │   │
│  │   - Confluent Cloud (recommended)                         │   │
│  │   - Azure Event Hubs (Kafka protocol)                     │   │
│  │   - Self-hosted on AKS                                    │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Step 1 - Azure Prerequisites

```bash
# 1. Install Azure CLI
# Windows: winget install Microsoft.AzureCLI
# Mac:     brew install azure-cli
# Linux:   curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# 2. Login
az login

# 3. Set your subscription (if you have multiple)
az account set --subscription "Your Subscription Name"

# 4. Register required providers (one-time)
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights
az provider register --namespace Microsoft.ContainerRegistry
az provider register --namespace Microsoft.DBforPostgreSQL
```

### Step 2 - Create Azure Resources

```bash
# Configuration - change these values
RESOURCE_GROUP="rg-document-notification-system"
LOCATION="eastus"
ACR_NAME="acrdocnotif$(openssl rand -hex 3)"  # Must be globally unique
ENVIRONMENT_NAME="env-document-notification"
PG_SERVER_NAME="pgdocnotif$(openssl rand -hex 3)"
PG_ADMIN_PASSWORD="$(openssl rand -base64 16)!"

echo "ACR: $ACR_NAME"
echo "PG Server: $PG_SERVER_NAME"
echo "PG Password: $PG_ADMIN_PASSWORD"
echo "Save these values!"

# Create Resource Group
az group create --name "$RESOURCE_GROUP" --location "$LOCATION"

# Create Container Registry
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --admin-enabled true

# Create PostgreSQL Flexible Server
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$PG_SERVER_NAME" \
  --location "$LOCATION" \
  --admin-user postgres \
  --admin-password "$PG_ADMIN_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 15 \
  --yes

# Allow Azure services to access PostgreSQL
az postgres flexible-server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$PG_SERVER_NAME" \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Initialize database schemas
PG_HOST="${PG_SERVER_NAME}.postgres.database.azure.com"
PGPASSWORD="$PG_ADMIN_PASSWORD" psql \
  -h "$PG_HOST" -U postgres -d postgres \
  -f infraestructure/docker-compose/init-db.sql

# Create Container Apps Environment
az containerapp env create \
  --name "$ENVIRONMENT_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION"
```

### Step 3 - Build and Push Images

Azure Container Registry can build images directly from your source code (no local Docker needed):

```bash
cd document-notification-system

# Get ACR credentials
ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

# Build all 4 images in parallel using ACR Tasks
az acr build --registry "$ACR_NAME" --image customer-service:latest \
  --file customer-service/Dockerfile . &

az acr build --registry "$ACR_NAME" --image document-service:latest \
  --file document-service/Dockerfile . &

az acr build --registry "$ACR_NAME" --image generator-service:latest \
  --file generator-service/Dockerfile . &

az acr build --registry "$ACR_NAME" --image notification-service:latest \
  --file notification-service/Dockerfile . &

wait
echo "All images built and pushed to $ACR_LOGIN_SERVER"
```

Alternatively, build locally and push:

```bash
# Login to ACR
az acr login --name "$ACR_NAME"

# Build locally
docker compose build

# Tag and push each image
for svc in customer-service document-service generator-service notification-service; do
  docker tag "document-notification-system-${svc}:latest" "${ACR_LOGIN_SERVER}/${svc}:latest"
  docker push "${ACR_LOGIN_SERVER}/${svc}:latest"
done
```

### Step 4 - Deploy Microservices

```bash
PG_HOST="${PG_SERVER_NAME}.postgres.database.azure.com"
PG_CONN="jdbc:postgresql://${PG_HOST}:5432/postgres?sslmode=require"

# --- Customer Service (1 instance, internal) ---
az containerapp create \
  --name customer-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "${ACR_LOGIN_SERVER}/customer-service:latest" \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_NAME" \
  --registry-password "$ACR_PASSWORD" \
  --target-port 8184 \
  --ingress internal \
  --min-replicas 1 --max-replicas 3 \
  --cpu 0.5 --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    SQL_INIT_MODE=never \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$PG_ADMIN_PASSWORD" \
    "SPRING_DATASOURCE_URL=${PG_CONN}&currentSchema=customer"

# --- Document Service (1 instance, external - this is the API entry point) ---
az containerapp create \
  --name document-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "${ACR_LOGIN_SERVER}/document-service:latest" \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_NAME" \
  --registry-password "$ACR_PASSWORD" \
  --target-port 8181 \
  --ingress external \
  --min-replicas 1 --max-replicas 3 \
  --cpu 0.5 --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    SQL_INIT_MODE=never \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$PG_ADMIN_PASSWORD" \
    "SPRING_DATASOURCE_URL=${PG_CONN}&currentSchema=document"

# --- Generator Service (scalable worker) ---
az containerapp create \
  --name generator-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "${ACR_LOGIN_SERVER}/generator-service:latest" \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_NAME" \
  --registry-password "$ACR_PASSWORD" \
  --target-port 8182 \
  --ingress internal \
  --min-replicas 1 --max-replicas 5 \
  --cpu 0.5 --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    SQL_INIT_MODE=never \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$PG_ADMIN_PASSWORD" \
    "SPRING_DATASOURCE_URL=${PG_CONN}&currentSchema=generator"

# --- Notification Service (scalable worker + email) ---
az containerapp create \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "${ACR_LOGIN_SERVER}/notification-service:latest" \
  --registry-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_NAME" \
  --registry-password "$ACR_PASSWORD" \
  --target-port 8183 \
  --ingress internal \
  --min-replicas 1 --max-replicas 10 \
  --cpu 0.5 --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    SQL_INIT_MODE=never \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$PG_ADMIN_PASSWORD" \
    "SPRING_DATASOURCE_URL=${PG_CONN}&currentSchema=notification" \
    "MAIL_FROM=${MAIL_FROM:-no-reply@example.com}" \
    "MAIL_USERNAME=${MAIL_USERNAME:-no-reply@example.com}" \
    "MAIL_PASSWORD=${MAIL_PASSWORD:-}"
```

### Step 5 - Configure Scaling Rules

Azure Container Apps supports automatic scaling based on HTTP traffic, CPU, memory, or custom metrics (like Kafka consumer lag).

#### Manual Scaling (set fixed replica count)

```bash
# Scale notification-service to exactly 3 replicas
az containerapp update \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --min-replicas 3 --max-replicas 3

# Scale generator-service to 2 replicas
az containerapp update \
  --name generator-service \
  --resource-group "$RESOURCE_GROUP" \
  --min-replicas 2 --max-replicas 2
```

#### Auto-Scaling by CPU (for workers)

```bash
# Scale notification-service from 1 to 10 instances when CPU > 60%
az containerapp update \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --min-replicas 1 --max-replicas 10 \
  --scale-rule-name cpu-scaling \
  --scale-rule-type cpu \
  --scale-rule-metadata "type=Utilization" "value=60"
```

#### Auto-Scaling by HTTP (for API services)

```bash
# Scale document-service based on concurrent HTTP requests
az containerapp update \
  --name document-service \
  --resource-group "$RESOURCE_GROUP" \
  --min-replicas 1 --max-replicas 5 \
  --scale-rule-name http-scaling \
  --scale-rule-type http \
  --scale-rule-metadata "concurrentRequests=50"
```

#### Auto-Scaling by Kafka Consumer Lag (advanced)

If using KEDA-compatible Kafka (Confluent Cloud or Event Hubs):

```bash
az containerapp update \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --min-replicas 1 --max-replicas 10 \
  --scale-rule-name kafka-lag \
  --scale-rule-type kafka \
  --scale-rule-metadata \
    "bootstrapServers=your-kafka-bootstrap:9092" \
    "consumerGroup=notification-topic-consumer" \
    "topic=notification-request" \
    "lagThreshold=100"
```

#### View Current Scaling Configuration

```bash
# Check replicas and scaling rules for all services
for svc in customer-service document-service generator-service notification-service; do
  echo "=== $svc ==="
  az containerapp show --name "$svc" --resource-group "$RESOURCE_GROUP" \
    --query "{replicas: properties.template.scale, runningInstances: properties.runningStatus}" \
    -o table
done
```

### Step 6 - Configure Kafka

The microservices need a Kafka cluster. Choose one option:

#### Option A: Confluent Cloud (recommended)

1. Create a cluster at [confluent.cloud](https://confluent.cloud)
2. Create API key and get bootstrap servers
3. Create Schema Registry and get URL + credentials
4. Create topics: `customer`, `generator-request`, `generator-response`, `notification-request`, `notification-response`
5. Update all services:

```bash
KAFKA_SERVERS="pkc-xxxxx.eastus.azure.confluent.cloud:9092"
SCHEMA_URL="https://psrc-xxxxx.eastus.azure.confluent.cloud"

for svc in document-service generator-service notification-service customer-service; do
  az containerapp update \
    --name "$svc" \
    --resource-group "$RESOURCE_GROUP" \
    --set-env-vars \
      "KAFKA_CONFIG_BOOTSTRAP_SERVERS=${KAFKA_SERVERS}" \
      "KAFKA_CONFIG_SCHEMA_REGISTRY_URL=${SCHEMA_URL}" \
      "KAFKA_CONFIG_SCHEMA_REGISTRY_URL_KEY=schema.registry.url"
done
```

#### Option B: Azure Event Hubs (Kafka-compatible)

```bash
# Create Event Hubs namespace with Kafka enabled
az eventhubs namespace create \
  --name ehns-docnotif \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --sku Standard \
  --enable-kafka true

# Create event hubs (= Kafka topics)
for topic in customer generator-request generator-response notification-request notification-response; do
  az eventhubs eventhub create \
    --name "$topic" \
    --namespace-name ehns-docnotif \
    --resource-group "$RESOURCE_GROUP" \
    --partition-count 3
done

# Get connection string
EH_CONN=$(az eventhubs namespace authorization-rule keys list \
  --namespace-name ehns-docnotif \
  --resource-group "$RESOURCE_GROUP" \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString -o tsv)

echo "Event Hubs connection: $EH_CONN"
echo "Bootstrap servers: ehns-docnotif.servicebus.windows.net:9093"
```

> **Note:** Event Hubs does not include Schema Registry. You would need Confluent Schema Registry deployed separately or use JSON serialization instead of Avro.

### Monitoring and Logs

```bash
# Stream live logs from a service
az containerapp logs show \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --type console \
  --follow

# View system logs (scaling events, crashes)
az containerapp logs show \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --type system \
  --follow

# Check revision status and replica count
az containerapp revision list \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  -o table

# View all container apps status
az containerapp list \
  --resource-group "$RESOURCE_GROUP" \
  -o table

# Get the public URL for document-service
az containerapp show \
  --name document-service \
  --resource-group "$RESOURCE_GROUP" \
  --query "properties.configuration.ingress.fqdn" -o tsv
```

### Tear Down

```bash
# Delete everything (irreversible)
az group delete --name "$RESOURCE_GROUP" --yes --no-wait
```

---

## Environment Variables Reference

### All Services

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | (none) | Set to `docker` for container deployment |
| `POSTGRES_USER` | `postgres` | Database username |
| `POSTGRES_PASSWORD` | `admin` | Database password |
| `SQL_INIT_MODE` | `always` | Set to `never` in Docker (DB is initialized by `init-db.sql`) |
| `SPRING_DATASOURCE_URL` | (per profile) | Override full JDBC URL (useful for Azure) |

### Notification Service (additional)

| Variable | Default | Description |
|----------|---------|-------------|
| `NOTIFICATION_INSTANCE_ID` | `notification-1` | Unique ID per instance (auto-set in Docker Compose via `$HOSTNAME`) |
| `SERVER_PORT` | `8183` | HTTP port |
| `MAIL_FROM` | `no-reply@example.com` | Sender email address |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server host |
| `MAIL_PORT` | `587` | SMTP server port |
| `MAIL_USERNAME` | `no-reply@example.com` | SMTP authentication username |
| `MAIL_PASSWORD` | (empty) | SMTP authentication password |
| `MAIL_RATE_LIMIT_TOKENS` | `5` | Max emails per interval |
| `MAIL_RATE_LIMIT_REFILL_MS` | `20000` | Rate limit refill interval (ms) |

### Scaling (Docker Compose)

| Variable | Default | Description |
|----------|---------|-------------|
| `GENERATOR_REPLICAS` | `1` | Number of generator-service instances |
| `NOTIFICATION_REPLICAS` | `1` | Number of notification-service instances |

---

## Spring Profiles

| Profile | Usage | Connections |
|---------|-------|-------------|
| `default` | Local development (IDE / Maven) | `localhost:5434` (PG), `localhost:19092,29092,39092` (Kafka) |
| `docker` | Docker Compose / Azure | `postgres:5432` (PG), `kafka-broker-N:9092` (Kafka) |

Each service has `application-docker.yml` that overrides:
- PostgreSQL: `localhost:5434` -> `postgres:5432`
- Kafka brokers: `localhost:19092,29092,39092` -> `kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092`
- Schema Registry: `localhost:8081` -> `schema-registry:8081`

For Azure, the `SPRING_DATASOURCE_URL` env var overrides the Docker profile's datasource URL to point to the Azure PostgreSQL server.

---

## Project Structure

```
document-notification-system/
├── docker-compose.yml                  # Full local orchestration
├── .dockerignore                       # Optimizes Docker build context
├── .env                                # Local env vars (create this file)
├── DOCKER-DEPLOYMENT.md                # This guide
│
├── azure/
│   └── deploy.sh                       # Automated Azure deployment script
│
├── infraestructure/docker-compose/
│   ├── init-db.sql                     # Consolidated DB init (all 4 schemas)
│   ├── .env                            # Kafka version config
│   ├── common.yml                      # Docker network definition
│   ├── zookeeper.yml                   # Zookeeper configuration
│   ├── kafka_cluster.yml               # 3-broker Kafka cluster
│   └── init_kafka.yml                  # Topic creation script
│
├── document-service/
│   ├── Dockerfile                      # Multi-stage build (Maven + JRE)
│   └── document-container/src/main/resources/
│       ├── application.yml             # Default profile (localhost)
│       └── application-docker.yml      # Docker profile (service hostnames)
│
├── generator-service/
│   ├── Dockerfile
│   └── generator-container/src/main/resources/
│       ├── application.yml
│       └── application-docker.yml
│
├── notification-service/
│   ├── Dockerfile
│   └── notification-container/src/main/resources/
│       ├── application.yml
│       └── application-docker.yml
│
└── customer-service/
    ├── Dockerfile
    └── customer-container/src/main/resources/
        ├── application.yml
        └── application-docker.yml
```
