# Escalera de pruebas: topologías × tamaños

Plan compacto para medir escalabilidad y rendimiento en Azure con la BD en `Standard_B1ms`
(`max_connections = 50`). Tres topologías, cuatro tamaños, **seis corridas** en total: cada topología
solo corre los escalones que aportan información nueva.

Los `.jmx` de esta carpeta son copias de [`../create-document.jmx`](../create-document.jmx) con la URL de
Azure y los hilos/loops ya fijados por defecto, así se abren directo en la GUI o se lanzan con
[`run-escalon.sh`](run-escalon.sh) sin `-J`. Siguen aceptando `-JbaseUrl`, `-Jthreads`, etc. para
sobreescribir.

## Tamaños

| Plan | Total | `threads` | `rampUp` | `loops` | Qué mide |
|---|---|---|---|---|---|
| `01-500-create-document.jmx` | 500 | 10 | 20 s | 50 | Línea base de latencia con contenedores calientes |
| `02-2000-create-document.jmx` | 2000 | 20 | 40 s | 100 | Concurrencia real sostenida |
| `03-5000-create-document.jmx` | 5000 | 25 | 60 s | 200 | Presión sobre Kafka y el outbox |
| `04-20000-create-document.jmx` | 20000 | 40 | 120 s | 500 | Techo. Exige `MP_MAX_MESSAGES ≥ 30000` en Mailpit |

Fijos en todos: `thinkTime=0`, `connectTimeout=10000`, `responseTimeout=60000`, mismo `customerId`.
Heap de JMeter: `export HEAP="-Xms1g -Xmx2g"` (el script ya lo hace).

## Topologías y qué corre cada una

Presupuesto de conexiones: `Σ réplicas × pool ≤ 45` (5 reservadas). El rolling restart duplica la
demanda un rato, así que **escalar entre topologías con el pipeline drenado y esperar a que la revisión
vieja desaparezca** de `az containerapp revision list`.

| Topología | document | generator | notification | customer | Pool | Conexiones | Escalones |
|---|---|---|---|---|---|---|---|
| **A** `1x1x1x1` | 1 | 1 | 1 | 1 | 5 | 20 | 500, 5000, 20000 |
| **B** `2x2x2` | 2 | 2 | 2 | 1 | 5 | 35 | 5000, 20000 |
| **C** `3x3x3` | 3 | 3 | 3 | 1 | 3 en consumidores | 15+9+9+5 = 38 | 20000 |

- **A** es la referencia: mide el techo de una sola instancia por servicio y el tiempo de drenaje del
  pipeline con un solo consumidor por topic. El escalón de 500 es calentamiento y línea base; se salta
  el de 2000 porque con una instancia 5000 ya muestra la cola.
- **B** dobla consumidores. `notification-request` tiene 6 particiones y cada réplica levanta 3 hilos,
  así que 2 réplicas = 6 hilos = las 6 particiones: es el punto de escalado natural de notification.
- **C** triplica. Los hilos de más en notification (9 sobre 6 particiones) quedan ociosos; lo que se
  mide aquí es document-service y generator. Si a 20000 el p95 o el drenaje no mejoran respecto a B,
  el cuello ya no es de réplicas sino de BD (`B1ms`, 1 vCPU) o de Confluent.

Escalón 2000 queda como comodín: úsalo en A si 500 salió bien pero 5000 tiene errores, para ubicar el
punto de quiebre sin gastar una corrida de 20000.

## Comandos por topología

```bash
RG=dns-student-rg

# A: 1x1x1x1 (estado actual)
for s in document-service generator-service notification-service customer-service; do
  az containerapp update -g $RG -n $s --min-replicas 1 --max-replicas 1 \
    --set-env-vars SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=5
done

# B: 2x2x2 (+ customer 1)
for s in document-service generator-service notification-service; do
  az containerapp update -g $RG -n $s --min-replicas 2 --max-replicas 2
done

# C: 3x3x3, pool 3 en los consumidores para caber en 45 conexiones
az containerapp update -g $RG -n document-service --min-replicas 3 --max-replicas 3
for s in generator-service notification-service; do
  az containerapp update -g $RG -n $s --min-replicas 3 --max-replicas 3 \
    --set-env-vars SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=3
done

# Verificar antes de lanzar: todas Running, una sola revisión activa por app
az containerapp list -g $RG --query "[].{n:name, min:properties.template.scale.minReplicas, max:properties.template.scale.maxReplicas, s:properties.runningStatus}" -o table
```

Al terminar C, volver a A con el primer bloque (deja pool 5 y una réplica) para no gastar capa gratuita.

## Correr

```bash
cd docs/jmeter/escalera
chmod +x run-escalon.sh

# A
./run-escalon.sh A-1x1x1x1 01-500-create-document.jmx
./run-escalon.sh A-1x1x1x1 03-5000-create-document.jmx
./run-escalon.sh A-1x1x1x1 04-20000-create-document.jmx
# escalar a B, esperar revisión vieja fuera
./run-escalon.sh B-2x2x2 03-5000-create-document.jmx
./run-escalon.sh B-2x2x2 04-20000-create-document.jmx
# escalar a C
./run-escalon.sh C-3x3x3 04-20000-create-document.jmx
```

Variables opcionales del script: `BASE_URL`, `MP` (URL de Mailpit), `CUSTOMER_ID`, `HEAP`, y `DB_URL`
para que cuente filas `NOTIFICATION_PENDING` atascadas al final de cada escalón si tienes `psql`.

## Qué anotar por corrida

`run-escalon.sh` deja una fila en `target/jmeter/escalera.csv`
(`config,total,threads,loops,seg_jmeter,seg_drenaje,correos`) y el reporte HTML en
`target/jmeter/<config>/<total>/report/index.html`. Para la tabla final:

| Métrica | De dónde | Qué dice |
|---|---|---|
| Throughput API (req/s), p95, p99, errores | Reporte JMeter | Capacidad de `document-service` |
| Segundos de drenaje | Script | Capacidad del pipeline Kafka → generator → notification → correo. Es la métrica que debe bajar al añadir réplicas |
| Correos en Mailpit vs total | Script | **Debe ser igual.** Más = duplicados (no debería ocurrir con el claim-then-send). Menos = filas atascadas |
| `SMTPRejected` | Script | Debe ser 0 |
| Filas `NOTIFICATION_PENDING` antiguas | Script / SQL | Envíos que murieron entre claim y complete |

Criterio para pasar al siguiente escalón: errores < 1 %, sin timeouts, p95 que no crezca más del doble
y `correos == total`. Si 5000 en A ya falla, no lances 20000 en A: pasa a B.
