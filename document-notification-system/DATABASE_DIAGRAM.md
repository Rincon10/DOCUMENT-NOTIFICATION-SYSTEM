# 📊 DIAGRAMA DE BASE DE DATOS - Document Notification System

## Diagrama Entidad-Relación (ER) en ASCII

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          DOCUMENT NOTIFICATION SYSTEM                           │
│                              DATABASE DIAGRAM                                   │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐
│        SCHEMA: customer     │
└─────────────────────────────┘

    ┌──────────────────────────────────┐
    │         customers (1:N)          │
    ├──────────────────────────────────┤
    │ PK  │ id (UUID)                  │
    │     │ username (VARCHAR) UNIQUE  │
    │     │ first_name (VARCHAR)       │
    │     │ last_name (VARCHAR)        │
    │     │ created_at (TIMESTAMP)     │
    └──────────────────────────────────┘
              │
              │ (1:N relationship)
              │
              ▼

┌─────────────────────────────────────────────────┐
│          SCHEMA: document                       │
├─────────────────────────────────────────────────┤

    ┌──────────────────────────────────────┐
    │      documents (N:1 customer)        │
    ├──────────────────────────────────────┤
    │ PK  │ id (UUID)                      │
    │ FK  │ customer_id (UUID) ──→ @        │
    │     │ account_id (UUID)              │
    │     │ file_name (VARCHAR)            │
    │     │ file_path (VARCHAR)            │
    │     │ period_start_date (DATE)       │
    │     │ period_end_date (DATE)         │
    │     │ total_late_interest (NUMERIC)  │
    │     │ total_regular_interest         │
    │     │ total_amount (NUMERIC)         │
    │     │ created_by (VARCHAR)           │
    │     │ created_at (TIMESTAMP)         │
    │     │ updated_at (TIMESTAMP)         │
    │     │ document_status (ENUM)         │
    │     │ failure_messages (TEXT)        │
    └──────────────────────────────────────┘
         │                              │
         │ (1:1)                        │ (1:N)
         │                              │
         ▼                              ▼

    ┌──────────────────────┐    ┌────────────────────────────┐
    │  document_address    │    │   document_items (1:N)     │
    │  (1:1 documents)     │    ├────────────────────────────┤
    ├──────────────────────┤    │ PK │ id (BIGINT)           │
    │ PK │ id (UUID)       │    │ PK │ document_id (UUID) ──→│
    │ FK │ document_id ────┼───→│    │ item_id (UUID)       │
    │    │ postal_code     │    │    │ late_interest        │
    │    │ address_line    │    │    │ regular_interest     │
    │    │ city (VARCHAR)  │    │    │ sub_total            │
    │    │ country         │    └────────────────────────────┘
    └──────────────────────┘


    ┌───────────────────────────────────────────┐
    │   generation_outbox (Outbox Pattern)      │
    ├───────────────────────────────────────────┤
    │ PK  │ id (UUID)                           │
    │     │ saga_id (UUID) UNIQUE               │
    │     │ created_at (TIMESTAMP)              │
    │     │ processed_at (TIMESTAMP)            │
    │     │ type (VARCHAR)                      │
    │     │ payload (TEXT - JSON)               │
    │     │ saga_status (ENUM)                  │
    │     │   - STARTED                         │
    │     │   - SUCCEEDED                       │
    │     │   - FAILED                          │
    │     │   - COMPENSATED                     │
    │     │ document_status (ENUM)              │
    │     │ outbox_status (ENUM)                │
    │     │   - STARTED                         │
    │     │   - PROCESSED                       │
    │     │   - FAILED                          │
    │     │ version (INT) - Optimistic Locking  │
    └───────────────────────────────────────────┘


    ┌──────────────────────────────────────────┐
    │  notification_outbox (Outbox Pattern)    │
    ├──────────────────────────────────────────┤
    │ PK  │ id (UUID)                          │
    │     │ saga_id (UUID)                     │
    │ FK  │ document_id (UUID) ──────────────→ │
    │     │ created_at (TIMESTAMP)             │
    │     │ processed_at (TIMESTAMP)           │
    │     │ type (VARCHAR)                     │
    │     │ payload (TEXT - JSON)              │
    │     │ outbox_status (ENUM)               │
    │     │ version (INT) - Optimistic Locking │
    └──────────────────────────────────────────┘
         │
         │ (FK a documents)
         │
         └─────→ @ (referencia a documents.id)

```

---

## 📋 ÍNDICES CREADOS

### En tabla `documents`
```
- idx_document_customer_id
- idx_document_status
- idx_document_created_at
- idx_document_period_dates
```

### En tabla `document_address`
```
- idx_document_address_document_id
```

### En tabla `document_items`
```
- idx_document_items_document_id
- idx_document_items_item_id
```

### En tabla `generation_outbox`
```
- idx_generation_outbox_saga_id
- idx_generation_outbox_status
- idx_generation_outbox_created_at
- idx_generation_outbox_saga_status
```

### En tabla `notification_outbox`
```
- idx_notification_outbox_saga_id
- idx_notification_outbox_document_id
- idx_notification_outbox_status
- idx_notification_outbox_created_at
```

---

## 🔀 RELACIONES DETALLADAS

### 1. Customers ↔ Documents (1:N)
```
customers.id ←─→ documents.customer_id (FK)
  Tipo: One-to-Many
  Constraint: ON DELETE RESTRICT, ON UPDATE CASCADE
  Significado: Un cliente puede tener muchos documentos,
               pero no puedes eliminar un cliente que tenga documentos
```

### 2. Documents ↔ Document Address (1:1)
```
documents.id ←─→ document_address.document_id (FK UNIQUE)
  Tipo: One-to-One
  Constraint: ON DELETE CASCADE, ON UPDATE CASCADE
  Significado: Cada documento tiene una dirección,
               y cada dirección pertenece a un documento
```

### 3. Documents ↔ Document Items (1:N)
```
documents.id ←─→ document_items.document_id (FK)
  Tipo: One-to-Many
  Constraint: ON DELETE CASCADE, ON UPDATE CASCADE
  Significado: Un documento puede tener muchos items,
               y eliminar un documento elimina sus items
```

### 4. Documents ↔ Notification Outbox (1:N)
```
documents.id ←─→ notification_outbox.document_id (FK)
  Tipo: One-to-Many
  Constraint: ON DELETE CASCADE, ON UPDATE CASCADE
  Significado: Un documento puede tener muchos mensajes de notificación
```

---

## 📊 TIPOS DE DATOS

### ENUM Types

#### document_status
```
PENDING      → Esperando procesamiento
GENERATED    → Documento generado
SENT         → Documento enviado
CANCELLED    → Documento cancelado
CANCELLING   → En proceso de cancelación
```

#### saga_status
```
STARTED      → Saga iniciada
SUCCEEDED    → Saga completada exitosamente
FAILED       → Saga falló
COMPENSATED  → Saga compensada (rollback)
```

#### outbox_status
```
STARTED      → Mensaje creado, esperando procesamiento
PROCESSED    → Mensaje procesado y publicado
FAILED       → Procesamiento falló
```

---

## 🎯 FLUJO DE DATOS

### Crear un documento (Happy Path)

```
1. CLIENTE ENVÍA REQUEST
   POST /documents
   JSON: CreateDocumentCommand
         ├─ customerId
         ├─ labels (items)
         └─ documentInformation

2. DOCUMENT SERVICE
   ├─ Valida cliente existe
   ├─ Crea documento en BD
   │  documents.document_status = PENDING
   └─ Publica evento

3. GENERATION OUTBOX PATTERN
   ├─ Crea mensaje en generation_outbox
   │  outbox_status = STARTED
   │  saga_status = STARTED
   └─ Publica a Kafka

4. GENERATOR SERVICE
   ├─ Recibe mensaje
   ├─ Genera PDF/documento
   └─ Responde

5. DOCUMENT SERVICE - SAGA COMPLETION
   ├─ Recibe respuesta generador
   ├─ Actualiza documento
   │  documents.document_status = GENERATED
   ├─ Crea notificación outbox
   │  notification_outbox.outbox_status = STARTED
   └─ Publica a Kafka

6. NOTIFICATION SERVICE
   ├─ Recibe notificación
   ├─ Envía email/SMS
   └─ Marca como completado

7. CLIENTE RECIBE DOCUMENTO
   documents.document_status = SENT
```

---

## 🗄️ CARDINALIDADES

```
customers (1) ─────────────┬──────────────── (N) documents
                            │
                            ├─── (1) document_address
                            │
                            ├─── (N) document_items
                            │
                            └─── (N) notification_outbox

generation_outbox es independiente pero referencia a documents indirectamente
```

---

## 💾 PATRONES IMPLEMENTADOS

### 1. Outbox Pattern
```
Usado en:
- generation_outbox
- notification_outbox

Beneficio:
- Garantiza exactamente-una-entrega de eventos
- Transaccionalidad con BD
- Recuperación ante fallos
```

### 2. Saga Pattern
```
Usado en:
- generation_outbox con saga_status
- Coordina documento ↔ generador

Estados:
STARTED (1) → SUCCEEDED (✓)
STARTED (1) → FAILED → COMPENSATED
```

### 3. Optimistic Locking
```
Usado en:
- generation_outbox.version
- notification_outbox.version

Previene race conditions
```

---

## 🎨 LEYENDA

```
PK   = Primary Key (Clave primaria)
FK   = Foreign Key (Clave foránea)
@    = Referencia a otra tabla
→    = Relación
1:N  = Uno a muchos
1:1  = Uno a uno
```

---

## 📈 CAPACIDAD Y PERFORMANCE

### Estimaciones

```
Clientes:         < 1,000,000
Documentos:       < 10,000,000
Items por doc:    < 1,000
Outbox messages:  < 50,000,000 (pueden limpiarse periódicamente)

Total tamaño aprox: 500 GB - 1 TB (con documentos guardados)
```

### Optimizaciones incluidas

- ✓ Índices en columnas frecuentemente consultadas
- ✓ Índices en FK para joins rápidos
- ✓ Índices en outbox_status para polling
- ✓ Índices en created_at para ordenamiento temporal
- ✓ Primary keys eficientes (UUID)
- ✓ Foreign keys con ON DELETE CASCADE donde apropiado

---

## 🔒 INTEGRIDAD REFERENCIAL

### Restricciones principales

1. **FK documents.customer_id**
   - No puedes eliminar cliente con documentos
   - Actualizar customer.id actualiza en cascada

2. **FK document_address.document_id**
   - Eliminar documento elimina dirección (CASCADE)
   - Garantiza integridad 1:1

3. **FK document_items.document_id**
   - Eliminar documento elimina items (CASCADE)
   - Mantiene documentos completos

4. **FK notification_outbox.document_id**
   - Eliminar documento elimina mensajes de notificación
   - Limpieza automática

---

## 🔄 VISTA SIMPLIFICADA DE FLUJO

```
Customer
   ↓
   └─→ Creates Document
         ├─→ Save to DB
         ├─→ Create Outbox Message
         └─→ Publish to Kafka
              ↓
              └─→ Generator Service
                   ├─→ Generates File
                   └─→ Sends Response
                        ↓
                        └─→ Document Service
                             ├─→ Update Document Status
                             ├─→ Create Notification Message
                             └─→ Publish to Kafka
                                  ↓
                                  └─→ Notification Service
                                       └─→ Send to Customer
```

---

## 📝 NOTAS IMPORTANTES

1. **Esquemas separados**: `document` y `customer` están en esquemas distintos
   para mejor aislamiento y escalabilidad

2. **Sin vistas materializadas en init-database.sql**: 
   El script simplificado no incluye MVs. Use `init-schema.sql` si las necesita.

3. **Cascading deletes**:
   Cuidado al eliminar documentos, cascadea a items y direcciones.

4. **Optimistic locking**:
   El campo `version` previene race conditions en outbox.

5. **Payload como TEXT**:
   Los payloads se guardan como JSON en TEXT. Considera usar JSONB en el futuro para mejor query performance.

---

**Este diagrama refleja la estructura creada por init-database.sql**

Última actualización: 2026-02-24

