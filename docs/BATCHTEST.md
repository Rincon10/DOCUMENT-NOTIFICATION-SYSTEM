# Pruebas de carga en ambientes de nube

Guía para ejecutar pruebas masivas de `POST /documents` contra un despliegue en la nube (Azure Container Apps).
Para levantar y probar el sistema **en local**, usa
[`commands/01-LOCAL-EXECUTION.md`](../document-notification-system/commands/01-LOCAL-EXECUTION.md).

| | Local | Nube |
|---|---|---|
| Documento | [`commands/01-LOCAL-EXECUTION.md`](../document-notification-system/commands/01-LOCAL-EXECUTION.md) | este archivo |
| Base URL | `http://localhost:8181` | `https://document-service.<dominio>.eastus.azurecontainerapps.io` |
| Kafka | docker-compose (zookeeper + cluster) | Confluent Cloud |
| Correo | Mailpit (sin cuota) | ACS — **5/min y 10/hora**, no ampliable en dominio administrado |

---

## 1. Requisitos

- El FQDN público de `document-service`. Se obtiene con:

  ```bash
  az containerapp show -g "$RESOURCE_GROUP" -n document-service --query properties.configuration.ingress.fqdn -o tsv
  ```

- Un **customer que ya exista** en la base de datos. `POST /documents` valida el `customerId` contra la
  vista materializada `"document".customers`, así que un UUID inventado devuelve error. Crea uno primero
  vía `customer-service` y guarda su id.
- Para la sección de JMeter: [Apache JMeter 5.6+](https://jmeter.apache.org/download_jmeter.cgi) y Java 19.

```bash
export BASE_URL="https://document-service.<dominio>.eastus.azurecontainerapps.io"
export CUSTOMER_ID="<uuid-de-un-cliente-existente>"
```

---

## 2. Health check

Siempre empieza por aquí. En Container Apps con `--min-replicas 0` la app puede estar **dormida** y la
primera petición paga el arranque en frío (varios segundos).

```bash
curl "$BASE_URL/actuator/health"
```

Espera `{"status":"UP",...}`. Si responde lento o falla la primera vez, repite: la petición misma
despierta la réplica.

---

## 3. Smoke test — un solo documento

Mismo request que en local pero contra la URL pública y **sin** la cookie `JSESSIONID` (el endpoint no
usa sesión; esa cookie en los ejemplos viejos era un artefacto del cliente REST).

```bash
curl --request POST \
  --url "$BASE_URL/documents" \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/vnd.api.v1+json' \
  --data '{
  "customerId": "'"$CUSTOMER_ID"'",
  "labels": [
    {
      "itemId": "123e4567-e89b-12d3-a456-426614174001",
      "amount": 1500.00,
      "lateInterest": 45.00,
      "regularInterest": 30.00,
      "subTotal": 1575.00
    },
    {
      "itemId": "123e4567-e89b-12d3-a456-426614174002",
      "amount": 850.50,
      "lateInterest": 25.50,
      "regularInterest": 17.00,
      "subTotal": 893.00
    }
  ],
  "documentInformation": {
    "address": {
      "postalCode": "10001",
      "street": "123 Main Street",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "periodStartDate": "2026-01-01",
    "periodEndDate": "2026-01-31",
    "totalLateInterest": 70.50,
    "totalRegularInterest": 47.00,
    "totalAmount": 2468.00,
    "documentType": "PDF"
  }
}'
```

Respuesta esperada: **200** con

```json
{"documentStatus":"PENDING","message":"Document created successfully","accountId":"..."}
```

`PENDING` es correcto: la generación y la notificación son asíncronas vía Kafka. No confundas
"200 PENDING" con "el correo ya salió" — eso se verifica en el paso 6.

---

## 4. Subir los recursos de la base de datos

El `Standard_B1ms` de la capa gratuita trae **`max_connections = 50`** y **1 vCPU**. Como cada réplica abre
un pool de HikariCP, la cuenta que decide si la prueba arranca o revienta es:

```
Σ (réplicas × pool)  +  ~5 reservadas (superusuario + monitoreo de Azure)  ≤  max_connections
```

| Configuración | Conexiones | ¿Cabe en B1ms? |
|---|---|---|
| 4 servicios × 1 réplica × pool 5 | 20 | ✅ |
| 4 servicios × 3 réplicas × pool 3 | 36 | ✅ (ajustado) |
| 4 servicios × 3 réplicas × pool 5 | 60 | ❌ |
| 4 servicios × 3 réplicas × pool 10 | 120 | ❌ |

> ⚠️ **Un rolling restart duplica la demanda**: la revisión vieja retiene sus conexiones mientras la nueva
> arranca. Si en régimen ya estás en el tope, el despliegue es lo que te tumba, no la carga.
>
> Cuando falta cupo, el log **engaña**: dice `Unable to determine Dialect without JDBC metadata`, que parece
> un error de configuración de Hibernate. No lo es — Hibernate 6.5 tiene un bug donde el
> `NullPointerException` de `JdbcIsolationDelegate.sqlExceptionHelper()` se come la `SQLException` real del
> "too many connections". Si dos revisiones con imagen y variables idénticas se comportan distinto,
> sospecha del cupo, no del código.

### Subir la BD (antes de escalar los servicios)

```bash
az postgres flexible-server update -g dns-student-rg -n dns-student-pg \
  --sku-name Standard_D2s_v3 --tier GeneralPurpose
```

Reinicia el servidor (~5 min). Pasa a **2 vCPU, 8 GB y ~859 conexiones**, con lo que puedes ir a 3 réplicas
con pool 10 sin acercarte al límite. Cuesta **~$0.16/hora** mientras siga encendido.

Verifica que quedó aplicado:

```bash
az postgres flexible-server show -g dns-student-rg -n dns-student-pg \
  --query "{sku:sku.name, tier:sku.tier, state:state}" -o table
```

### Alternativa barata: subir solo `max_connections`

No cambia de SKU, así que **no arregla la CPU** — y en una prueba masiva el único núcleo del B1ms es el
cuello de botella real. Además la RAM es el techo: cada backend de Postgres cuesta ~9 MB, así que en 2 GB no
conviene pasar de ~150.

```bash
az postgres flexible-server parameter set -g dns-student-rg --server-name dns-student-pg \
  --name max_connections --value 200
az postgres flexible-server restart -g dns-student-rg -n dns-student-pg
```

El `restart` es **obligatorio**: `max_connections` es un parámetro estático (`isDynamicConfig: false`).

### Revertir a la BD barata (¡el mismo día!)

> ⚠️ Fuera del `Standard_B1ms` se cobra **~$115/mes**. Este es el comando que se olvida y el que cuesta
> dinero.

```bash
# 1. Servicios al modo ahorro
for s in document-service customer-service generator-service notification-service; do
  az containerapp update -g dns-student-rg -n $s --min-replicas 0 --max-replicas 1
done

# 2. BD de vuelta al tamaño gratuito
az postgres flexible-server update -g dns-student-rg -n dns-student-pg \
  --sku-name Standard_B1ms --tier Burstable

# 3. Pausar la BD hasta la próxima sesión
az postgres flexible-server stop -g dns-student-rg -n dns-student-pg
```

Confirma que no quedó nada encendido:

```bash
az containerapp list -g dns-student-rg \
  --query "[].{app:name, min:properties.template.scale.minReplicas}" -o table
az postgres flexible-server show -g dns-student-rg -n dns-student-pg \
  --query "{sku:sku.name, tier:sku.tier, state:state}" -o table
```

---

## 5. Prueba de carga con JMeter

El plan está en [`jmeter/create-document.jmx`](jmeter/create-document.jmx). Incluye un
`setUp Thread Group` que valida el health check antes de arrancar (si no da `UP`, aborta el test en vez
de generar cientos de errores), y asserts de status 200 + `documentStatus == PENDING`.

### Parámetros

Todos se pasan con `-J` y tienen default para correr sin configurar nada:

| Propiedad | Default | Qué controla |
|---|---|---|
| `baseUrl` | `http://localhost:8181` | URL base del `document-service` |
| `customerId` | `550e8400-...440001` | cliente existente al que se le crean documentos |
| `threads` | `10` | usuarios concurrentes |
| `rampUp` | `10` | segundos para levantar todos los hilos |
| `loops` | `50` | iteraciones por hilo (total = `threads` × `loops`) |
| `thinkTime` | `0` | ms de pausa entre peticiones (+ hasta 100 ms aleatorios) |
| `connectTimeout` | `10000` | ms de timeout de conexión |
| `responseTimeout` | `60000` | ms de timeout de respuesta |

El `itemId` de cada label se genera con `${__UUID()}`, así que cada petición es única y no choca con la
restricción de unicidad.

### Ejecución en modo CLI

Nunca corras una prueba de carga desde la GUI de JMeter — la GUI es solo para editar el plan.

```bash
jmeter -n \
  -t docs/jmeter/create-document.jmx \
  -JbaseUrl="$BASE_URL" \
  -JcustomerId="$CUSTOMER_ID" \
  -Jthreads=25 \
  -JrampUp=30 \
  -Jloops=20 \
  -l target/jmeter/results.jtl \
  -e -o target/jmeter/report
```

Esto genera 500 peticiones (25 × 20) y un **reporte HTML** en `target/jmeter/report/index.html` con
percentiles, throughput y gráficas de tiempo de respuesta.

> `target/jmeter/` debe estar vacío o no existir; JMeter se niega a sobreescribir un reporte anterior.
> Límpialo con `rm -rf target/jmeter` entre corridas.

### Escalón de carga sugerido

Sube por etapas y mira dónde se degrada, en vez de disparar el máximo de una:

```bash
for t in 5 10 25 50; do
  rm -rf target/jmeter/$t
  jmeter -n -t docs/jmeter/create-document.jmx \
    -JbaseUrl="$BASE_URL" -JcustomerId="$CUSTOMER_ID" \
    -Jthreads=$t -JrampUp=$t -Jloops=20 \
    -l target/jmeter/$t/results.jtl -e -o target/jmeter/$t/report
done
```

---

## 6. Qué verificar después de la carga

> ⚠️ **Antes de correr una prueba masiva: cambia el proveedor de correo a Mailpit.**
> ACS Email sobre un **Azure Managed Domain** admite **5 correos/minuto y 10/hora por suscripción**, y
> esos límites **no son ampliables** ([Microsoft solo sube cuota a dominios propios verificados](03-AZURE-ESTUDIANTE-PASO-A-PASO.md#76-opcional-dominio-propio-subir-la-cuota-de-10hora-a-100hora)). Con
> 10.000 documentos, `notification-service` acumularía 429s durante ~42 días. El proveedor SMTP no tiene
> esa cuota:
>
> ```bash
> az containerapp create -g "$RESOURCE_GROUP" -n mailpit --yaml document-notification-system/azure/mailpit.yaml
> az containerapp update -g "$RESOURCE_GROUP" -n notification-service >   --set-env-vars MAIL_PROVIDER=smtp MAIL_HOST=mailpit MAIL_PORT=1025 >     MAIL_SMTP_AUTH=false MAIL_SMTP_STARTTLS_ENABLE=false MAIL_SMTP_STARTTLS_REQUIRED=false >     MAIL_RATE_LIMIT_TOKENS=100 MAIL_RATE_LIMIT_REFILL_MS=1000
> ```
>
> Al terminar, volver a ACS con `MAIL_PROVIDER=azure` y `MAIL_RATE_LIMIT_TOKENS=10
> MAIL_RATE_LIMIT_REFILL_MS=3600000`, y borrar Mailpit. El detalle está en la
> [sección de escalado de la guía](03-AZURE-ESTUDIANTE-PASO-A-PASO.md#escalado-múltiples-instancias-manual-y-automático).


Un 200 solo confirma que el request se aceptó. El flujo completo se valida aguas abajo:

1. **Outbox drenado** — en Postgres, las filas de `"document".generation_outbox` y
   `"document".notification_outbox` deben pasar de `STARTED` a `COMPLETED`. Si se quedan atascadas,
   el scheduler del outbox o la conexión a Kafka son el problema:

   ```sql
   SELECT outbox_status, COUNT(*) FROM "document".notification_outbox GROUP BY outbox_status;
   ```

2. **Eventos en Kafka** — en la consola de Confluent, los topics `generator-request` y
   `notification-request` deben mostrar los mensajes entrando, cada uno con su `sagaId`.

3. **Correos enviados** — si apuntaste a Mailpit, revisa su UI web. Si dejaste ACS (solo válido para
   una prueba pequeña, dentro de los 10/hora), busca en los logs de `notification-service`
   `Email sent successfully to: ... | MessageId: ...`:

   ```bash
   az containerapp logs show -g "$RESOURCE_GROUP" -n notification-service --type console --follow
   ```

4. **Escalado (KEDA)** — cuántas réplicas levantó Container Apps durante el pico:

   ```bash
   az containerapp replica list -g "$RESOURCE_GROUP" -n document-service -o table
   ```

---

## 7. Problemas comunes

| Síntoma | Causa |
|---|---|
| `Could not find a replica for this app` al pedir logs | la app está dormida (`--min-replicas 0`); despiértala con una petición HTTP |
| Primeras peticiones muy lentas y luego normales | arranque en frío; usa un `rampUp` mayor o precalienta con el health check |
| Todas las peticiones fallan con error de validación | el `customerId` no existe en `"document".customers`; crea el cliente primero |
| `relation "customers" does not exist` (500) | falta la vista materializada; revisa la ejecución de `init-schema.sql` |
| Muchos timeouts al subir hilos | tope de réplicas o de conexiones de Postgres; revisa `max-replicas` y el pool de HikariCP |

---

## 8. Alternativa: Postman / Newman

La colección de Postman está en
[`commands/postman/create-document-collection.json`](../document-notification-system/commands/postman/create-document-collection.json)
y su salida de ejemplo en
[`commands/02-POSTMAN-CALLS.md`](../document-notification-system/commands/02-POSTMAN-CALLS.md).
Se puede correr en lote con Newman:

```bash
newman run create-document-collection.json -n 500 \
  --env-var "baseUrl=$BASE_URL" --env-var "customerId=$CUSTOMER_ID"
```

Newman sirve para validar aserciones funcionales en volumen; para métricas de carga (percentiles,
throughput, concurrencia real) usa el plan de JMeter del paso 5.
