# Diagramas del despliegue en Azure

Las cinco vistas de la arquitectura del **Document Notification System** desplegado en **Azure Container Apps** con cuenta de estudiante. Los comandos para construir cada pieza están en [`03-AZURE-ESTUDIANTE-PASO-A-PASO.md`](03-AZURE-ESTUDIANTE-PASO-A-PASO.md); las variables de entorno completas en [`02-VARIABLES-DEPLOYMENT.md`](02-VARIABLES-DEPLOYMENT.md).

---

## 1. Vista general del despliegue

Todo vive en el resource group `dns-student-rg`, salvo Kafka: la cuenta de estudiante no permite compras de Marketplace, así que Confluent Cloud se contrata directo, fuera de Azure. El correo sale por **Azure Communication Services** (API HTTPS, sin SMTP).

```mermaid
flowchart TB
    User(["👤 Usuario / Postman"])

    subgraph Azure["☁️ Azure — resource group: dns-student-rg"]
        ACR["📦 Azure Container Registry<br/>dnsstudentacr.azurecr.io<br/>(imágenes Docker · ~$5/mes)"]

        subgraph Env["Container Apps Environment: dns-student-env (red privada · gratis)"]
            DOC["document-service :8181<br/>ingress: external<br/>min 0 / max 1"]
            CUS["customer-service :8184<br/>ingress: external<br/>min 0 / max 1"]
            GEN["generator-service :8182<br/>ingress: internal<br/>min 0 / max 1"]
            NOT["notification-service :8183<br/>ingress: internal<br/>min 0 / max 1"]
        end

        PG[("🗄️ PostgreSQL Flexible Server<br/>B1ms · 32 GB<br/>gratis 12 meses")]

        ACS["✉️ Azure Communication Services<br/>Email API HTTPS<br/>donotreply@&lt;guid&gt;.azurecomm.net"]
    end

    subgraph Confluent["☁️ Confluent Cloud (registro directo, fuera de Azure)"]
        KAFKA["Kafka cluster Basic<br/>5 topics × 3 particiones"]
        SR["Schema Registry<br/>(esquemas Avro)"]
    end

    User -- "HTTPS público" --> DOC
    User -- "HTTPS público" --> CUS
    ACR -. "descarga imágenes al arrancar" .-> Env
    DOC & CUS & GEN & NOT -- "JDBC + SSL" --> PG
    DOC & CUS & GEN & NOT -- "SASL_SSL" --> KAFKA
    DOC & CUS & GEN & NOT -.-> SR
    NOT -- "envía correos (API HTTPS)" --> ACS
```

> 💰 **Costo:** con scale-to-zero y la capa gratuita, un semestre de demos cuesta entre $0 y $10 de los $100 de crédito. Lo único que cobra por existir es el ACR (~$5/mes).

## 2. Topología de red y seguridad

Solo los dos servicios con API pública son alcanzables desde internet; los consumidores de Kafka quedan en la red privada del environment. Toda credencial vive como secreto y las variables solo la referencian (`secretref:`).

```mermaid
flowchart LR
    subgraph Internet
        U["👤 Internet"]
    end
    subgraph Env["Container Apps Environment (red privada)"]
        direction TB
        EXT["ingress EXTERNAL<br/>document-service<br/>customer-service"]
        INT["ingress INTERNAL<br/>generator-service<br/>notification-service"]
    end
    SEC["🔐 Secrets de Container Apps<br/>pgpass · kafkajaas · srauth · acsconn"]

    U -- "solo HTTPS 443" --> EXT
    U -. "❌ sin acceso" .-> INT
    SEC -. "secretref: en env vars" .-> Env
```

> 🔒 **Cifrado en tránsito:** JDBC con `sslmode=require`, Kafka con `SASL_SSL`, correo por HTTPS hacia ACS.

## 3. Del código a la nube

`az acr build` compila la imagen en Azure (no hace falta Docker local). Con `min-replicas 0`, un servicio sin tráfico se apaga solo y no cobra nada mientras duerme.

```mermaid
flowchart LR
    Code["💻 Tu PC<br/>código fuente"] -- "az acr build<br/>(compila EN la nube)" --> ACR["📦 ACR<br/>imagen :1.0"]
    ACR -- "az containerapp create/update" --> APP["🚀 Container App<br/>corriendo"]
    APP -- "min-replicas 0<br/>sin tráfico → duerme" --> ZZZ["😴 0 réplicas<br/>= $0"]
    ZZZ -- "llega un request<br/>(~15-30 s cold start)" --> APP
```

## 4. Flujo de negocio sobre esta infraestructura

El mismo flujo de eventos que en local, con ACS como transporte del correo. Si `generator` o `notification` están dormidos, los eventos esperan en Kafka sin perderse: el flujo se pausa, no se rompe.

```mermaid
sequenceDiagram
    actor U as Usuario
    participant D as document-service
    participant K as Kafka (Confluent)
    participant G as generator-service
    participant N as notification-service
    participant A as ACS Email (Azure)

    U->>D: POST /documents (HTTPS público)
    D->>K: evento generator-request (Avro)
    K->>G: consume
    Note over G: genera el documento
    G->>K: evento generator-response
    K->>D: consume → actualiza estado
    D->>K: evento notification-request
    K->>N: consume
    N->>A: envía el correo (API HTTPS)
    N->>K: evento notification-response
```

## 5. Escalado: particiones y réplicas

La regla de oro: **réplicas útiles ≤ particiones del topic**. Con 3 particiones, hasta 3 réplicas por consumidor reparten el trabajo — de forma manual (`--min/max-replicas`) o automática con reglas KEDA (concurrencia HTTP en las APIs, lag de Kafka en los consumidores). Detalle completo en la [sección de escalado de la guía](03-AZURE-ESTUDIANTE-PASO-A-PASO.md#escalado-múltiples-instancias-manual-y-automático).

```mermaid
flowchart LR
    subgraph Topic["topic: notification-request (3 particiones)"]
        P0["partición 0"]
        P1["partición 1"]
        P2["partición 2"]
    end
    subgraph CG["consumer group: notification-service"]
        R1["réplica 1"]
        R2["réplica 2"]
        R3["réplica 3"]
    end
    P0 --> R1
    P1 --> R2
    P2 --> R3
```

> ⚠️ **Antes de escalar:** `SQL_INIT_MODE=never` en los 4 servicios y ojo con la BD — el B1ms trae `max_connections = 50` y cada réplica abre un pool de 5, así que con los consumidores en 3 réplicas ya estás en el tope (y un rolling restart lo duplica). Para pruebas, subir a `D2s_v3` unas horas y volver a B1ms el mismo día.