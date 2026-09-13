# Escalera de pruebas: topologías × tamaños

Plan compacto para medir escalabilidad y rendimiento en Azure (BD en `Standard_B4ms` desde el 13 de
septiembre; con `B1ms` aplica el presupuesto de 45 conexiones). Tres topologías, cuatro tamaños, **seis corridas** en total: cada topología
solo corre los escalones que aportan información nueva. La última repite la topología de la
[corrida del 9 de septiembre](../../../README.md#resultados-obtenidos-9-de-septiembre-de-2026) para
comparar contra sus 22.278 correos.

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

### Un CSV por corrida, abrible en Excel

El Summary Report de cada plan escribe su propio archivo, una fila por petición, sin pasar `-l`:

```
configuracion-servicios/<config>/<total>/<num>-<total>-<config>-<fecha>.csv
p. ej. configuracion-servicios/1-1-1/5000/03-5000-1-1-1-20260913-120904.csv
```

- `config` identifica la topología y es el nombre de su carpeta (`-Jconfig=2-2-2`; por defecto `1-1-1`). Así el mismo plan de
  20000 deja tres archivos distintos al correrlo en A, B y C.
- `resultsDir` cambia la carpeta base (`-JresultsDir=...`; por defecto `results`, relativa al directorio
  desde donde se lanza JMeter). `run-escalon.sh` la fija a `docs/jmeter/escalera/configuracion-servicios`, donde
  cada topología tiene su carpeta con las capturas de Azure (`1-1-1/`, `2-2-2/`, `3-6-6/`) y un `README.md` con sus resultados.
- La fecha evita sobrescribir si repites un escalón.
- Desde la GUI, cambia el valor por defecto de `config` en *User Defined Variables* antes de darle Start,
  o el archivo se llamará `1-1-1` aunque estés en otra topología.

El CSV trae `timeStamp, elapsed, label, responseCode, success, Latency, Connect, allThreads, ...`. En Excel:
`elapsed` es la latencia en ms para percentiles (`=PERCENTIL.INC(B:B;0,95)`), `success` para la tasa de
error, y `timeStamp` (epoch ms) para el throughput. Es el mismo formato de los CSV de
[`docs/pruebas-rendimiento/`](../../pruebas-rendimiento/).

## Topologías y qué corre cada una

Presupuesto de conexiones: `Σ réplicas × pool ≤ 45` (5 reservadas). El rolling restart duplica la
demanda un rato, así que **escalar entre topologías con el pipeline drenado y esperar a que la revisión
vieja desaparezca** de `az containerapp revision list`.

| Topología | document | generator | notification | customer | Pool | Conexiones | Escalones |
|---|---|---|---|---|---|---|---|
| **A** `1-1-1` | 1 | 1 | 1 | 1 | 5 | 20 | 500, 5000, 20000 |
| **B** `2-2-2` | 2 | 2 | 2 | 1 | 5 | 35 | 5000, 20000 |
| **C** `3-6-6` | 3 | 6 | 6 | 1 | 5 (BD en `B4ms`) | 15+30+30+5 = 80 | 20000 |

> El 13 de septiembre la BD se subió a `Standard_B4ms` antes de empezar la escalera, así que el presupuesto
> de 45 conexiones del `B1ms` ya no aplica y C corre con pool 5, igual que la corrida original. Si vuelves a
> `B1ms`, baja el pool de los consumidores a 2 en C (`3x5 + 6x2 + 6x2 + 5 = 44`).

- **A** es la referencia: mide el techo de una sola instancia por servicio y el tiempo de drenaje del
  pipeline con un solo consumidor por topic. El escalón de 500 es calentamiento y línea base; se salta
  el de 2000 porque con una instancia 5000 ya muestra la cola.
- **B** dobla consumidores. `notification-request` tiene 6 particiones y cada réplica levanta 3 hilos,
  así que 2 réplicas = 6 hilos = las 6 particiones: es el punto de escalado natural de notification.
- **C** es la **misma topología de la corrida del 9 de septiembre** (document 3 · generator 6 ·
  notification 6), la que dio 22.278 correos para 20.000 peticiones. Repetirla con el fix es la
  comparación directa: mismo tamaño, misma topología, el conteo de Mailpit debe ser exactamente 20.000.
  Con 6 réplicas de notification hay 18 hilos para 6 particiones; los sobrantes quedan ociosos, así que
  el drenaje no debería mejorar mucho respecto a B. La BD está en `Standard_B4ms` como en aquella corrida,
  así que la comparación de latencias también es directa.

Escalón 2000 queda como comodín: úsalo en A si 500 salió bien pero 5000 tiene errores, para ubicar el
punto de quiebre sin gastar una corrida de 20000.

## Comandos por topología

```bash
RG=dns-student-rg

# A: 1-1-1 (estado actual)
for s in document-service generator-service notification-service customer-service; do
  az containerapp update -g $RG -n $s --min-replicas 1 --max-replicas 1 \
    --set-env-vars SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=5
done

# B: 2-2-2 (+ customer 1)
for s in document-service generator-service notification-service; do
  az containerapp update -g $RG -n $s --min-replicas 2 --max-replicas 2
done

# C: 3-6-6 (la topología de la corrida del 9 de septiembre). Con la BD en B4ms el pool se queda en 5.
az containerapp update -g $RG -n document-service --min-replicas 3 --max-replicas 3
for s in generator-service notification-service; do
  az containerapp update -g $RG -n $s --min-replicas 6 --max-replicas 6
done
# Solo si la BD sigue en B1ms (45 conexiones utiles): añadir a los dos consumidores
#   --set-env-vars SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=2

# Verificar antes de lanzar: todas Running, una sola revisión activa por app
az containerapp list -g $RG --query "[].{n:name, min:properties.template.scale.minReplicas, max:properties.template.scale.maxReplicas, s:properties.runningStatus}" -o table
```

Al terminar C, volver a A con el primer bloque (deja pool 5 y una réplica) para no gastar capa gratuita.

## Correr

```bash
cd docs/jmeter/escalera
chmod +x run-escalon.sh

# A
./run-escalon.sh 1-1-1 01-500-create-document.jmx
./run-escalon.sh 1-1-1 03-5000-create-document.jmx
./run-escalon.sh 1-1-1 04-20000-create-document.jmx
# escalar a B, esperar revisión vieja fuera
./run-escalon.sh 2-2-2 03-5000-create-document.jmx
./run-escalon.sh 2-2-2 04-20000-create-document.jmx
# escalar a C
./run-escalon.sh 3-6-6 04-20000-create-document.jmx
```

Variables opcionales del script: `BASE_URL`, `MP` (URL de Mailpit), `CUSTOMER_ID`, `HEAP`, `RESULTS_DIR`
y `DB_URL` para que cuente filas `NOTIFICATION_PENDING` atascadas al final de cada escalón si tienes `psql`.
JMeter no está en el `PATH` de esta máquina: `export PATH="/c/apache-jmeter-5.6.3/apache-jmeter-5.6.3/bin:$PATH"`.

**Health check inicial.** El Header Manager global del plan manda `Accept: application/vnd.api.v1+json`, que
Actuator no puede producir, así que `/actuator/health` respondía 500 y el `setUp` abortaba la prueba. Los planes
de esta carpeta llevan un Header Manager propio en ese sampler con `Accept: application/json` y el `setUp` en
`continue`: si el health falla, queda registrado en el CSV pero la carga se ejecuta igual.

## Qué anotar por corrida

Todo queda versionable dentro de `configuracion-servicios/`: `run-escalon.sh` deja una fila por escalón en
`configuracion-servicios/escalera.csv` (`config,total,threads,loops,seg_jmeter,seg_drenaje,correos,fecha,nota`; la nota
sale de la variable `NOTA` si la defines), el reporte HTML en `configuracion-servicios/<config>/<total>/report/index.html`
y el `.jtl` crudo al lado. Para la tabla final:

| Métrica | De dónde | Qué dice |
|---|---|---|
| Throughput API (req/s), p95, p99, errores | Reporte JMeter | Capacidad de `document-service` |
| Segundos de drenaje | Script | Capacidad del pipeline Kafka → generator → notification → correo. Es la métrica que debe bajar al añadir réplicas |
| Correos en Mailpit vs total | Script | **Debe ser igual.** Más = duplicados (no debería ocurrir con el claim-then-send). Menos = filas atascadas |
| `SMTPRejected` | Script | Debe ser 0 |
| Filas `NOTIFICATION_PENDING` antiguas | Script / SQL | Envíos que murieron entre claim y complete |

Criterio para pasar al siguiente escalón: errores < 1 %, sin timeouts, p95 que no crezca más del doble
y `correos == total`. Si 5000 en A ya falla, no lances 20000 en A: pasa a B.
