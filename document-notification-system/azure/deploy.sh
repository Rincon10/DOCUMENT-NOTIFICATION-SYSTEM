#!/bin/bash
# ===================================================================
# DOCUMENT NOTIFICATION SYSTEM - Azure Container Apps Deployment
# ===================================================================
#
# Prerequisites:
#   1. Azure CLI installed: https://docs.microsoft.com/en-us/cli/azure/install-azure-cli
#   2. Logged in: az login
#   3. Docker images pushed to Azure Container Registry (ACR)
#
# Usage:
#   chmod +x deploy.sh
#   ./deploy.sh
#
# ===================================================================

set -euo pipefail

# --- Configuration (edit these) ---
RESOURCE_GROUP="rg-document-notification-system"
LOCATION="eastus"
ACR_NAME="acrdocnotif"                          # Must be globally unique, lowercase
ENVIRONMENT_NAME="env-document-notification"
POSTGRES_ADMIN_PASSWORD="Ch@ngeMe123!"          # Change in production!
MAIL_USERNAME="${MAIL_USERNAME:-no-reply@example.com}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"

echo "=========================================="
echo "  Document Notification System - Azure Deploy"
echo "=========================================="

# --- 1. Resource Group ---
echo "[1/8] Creating resource group..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none

# --- 2. Azure Container Registry ---
echo "[2/8] Creating Container Registry..."
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --admin-enabled true \
  --output none

ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

# --- 3. Build & Push Docker images to ACR ---
echo "[3/8] Building and pushing Docker images to ACR..."
echo "  This may take several minutes on first build..."

cd "$(dirname "$0")/.."

az acr build --registry "$ACR_NAME" --image document-service:latest --file document-service/Dockerfile . &
az acr build --registry "$ACR_NAME" --image generator-service:latest --file generator-service/Dockerfile . &
az acr build --registry "$ACR_NAME" --image notification-service:latest --file notification-service/Dockerfile . &
az acr build --registry "$ACR_NAME" --image customer-service:latest --file customer-service/Dockerfile . &
wait
echo "  All images built and pushed."

# --- 4. Azure PostgreSQL Flexible Server ---
echo "[4/8] Creating PostgreSQL Flexible Server..."
az postgres flexible-server create \
  --resource-group "$RESOURCE_GROUP" \
  --name "pgdocnotif" \
  --location "$LOCATION" \
  --admin-user postgres \
  --admin-password "$POSTGRES_ADMIN_PASSWORD" \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 15 \
  --yes \
  --output none

# Allow Azure services to connect
az postgres flexible-server firewall-rule create \
  --resource-group "$RESOURCE_GROUP" \
  --name "pgdocnotif" \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0 \
  --output none

# Run init SQL
POSTGRES_HOST="pgdocnotif.postgres.database.azure.com"
echo "  Initializing database schemas..."
PGPASSWORD="$POSTGRES_ADMIN_PASSWORD" psql \
  -h "$POSTGRES_HOST" \
  -U postgres \
  -d postgres \
  -f infraestructure/docker-compose/init-db.sql

# --- 5. Container Apps Environment ---
echo "[5/8] Creating Container Apps environment..."
az containerapp env create \
  --name "$ENVIRONMENT_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none

# --- 6. Deploy Kafka (via Confluent Cloud or self-hosted) ---
echo "[6/8] Kafka setup..."
echo "  NOTE: For production, use Confluent Cloud or Azure Event Hubs with Kafka protocol."
echo "  This script deploys the microservices. Configure KAFKA_BOOTSTRAP_SERVERS manually."
echo ""
echo "  Option A: Confluent Cloud (recommended)"
echo "    - Create cluster at https://confluent.cloud"
echo "    - Get bootstrap servers and API key"
echo ""
echo "  Option B: Azure Event Hubs (Kafka-compatible)"
echo "    - az eventhubs namespace create --name ehns-docnotif --resource-group $RESOURCE_GROUP"
echo ""

KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092}"
SCHEMA_REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://schema-registry:8081}"

# --- 7. Deploy microservices ---
echo "[7/8] Deploying microservices..."

# Common environment variables
COMMON_ENV="\
SPRING_PROFILES_ACTIVE=docker \
POSTGRES_USER=postgres \
POSTGRES_PASSWORD=$POSTGRES_ADMIN_PASSWORD \
SQL_INIT_MODE=never"

# Customer Service
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
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$POSTGRES_ADMIN_PASSWORD" \
    SQL_INIT_MODE=never \
    "SPRING_DATASOURCE_URL=jdbc:postgresql://${POSTGRES_HOST}:5432/postgres?currentSchema=customer&sslmode=require" \
  --output none

# Document Service
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
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$POSTGRES_ADMIN_PASSWORD" \
    SQL_INIT_MODE=never \
    "SPRING_DATASOURCE_URL=jdbc:postgresql://${POSTGRES_HOST}:5432/postgres?currentSchema=document&sslmode=require" \
  --output none

# Generator Service
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
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$POSTGRES_ADMIN_PASSWORD" \
    SQL_INIT_MODE=never \
    "SPRING_DATASOURCE_URL=jdbc:postgresql://${POSTGRES_HOST}:5432/postgres?currentSchema=generator&sslmode=require" \
  --output none

# Notification Service
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
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=docker \
    POSTGRES_USER=postgres \
    "POSTGRES_PASSWORD=$POSTGRES_ADMIN_PASSWORD" \
    SQL_INIT_MODE=never \
    "SPRING_DATASOURCE_URL=jdbc:postgresql://${POSTGRES_HOST}:5432/postgres?currentSchema=notification&sslmode=require" \
    "MAIL_FROM=${MAIL_USERNAME}" \
    "MAIL_USERNAME=${MAIL_USERNAME}" \
    "MAIL_PASSWORD=${MAIL_PASSWORD}" \
  --output none

# --- 8. Summary ---
echo "[8/8] Deployment complete!"
echo ""
echo "=========================================="
echo "  DEPLOYMENT SUMMARY"
echo "=========================================="
echo ""

DOCUMENT_URL=$(az containerapp show --name document-service --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || echo "pending...")

echo "  Resource Group:   $RESOURCE_GROUP"
echo "  ACR:              $ACR_LOGIN_SERVER"
echo "  PostgreSQL:       $POSTGRES_HOST"
echo "  Document API:     https://$DOCUMENT_URL"
echo ""
echo "  IMPORTANT: Configure Kafka bootstrap servers for each container app."
echo "  Use 'az containerapp update' to set KAFKA_BOOTSTRAP_SERVERS."
echo ""
echo "  To tear down all resources:"
echo "    az group delete --name $RESOURCE_GROUP --yes --no-wait"
echo "=========================================="
