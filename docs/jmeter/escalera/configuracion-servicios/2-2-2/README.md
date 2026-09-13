# Topología 2-2-2: dos instancias por servicio

Corrida del 13 de septiembre de 2026 con el fix *claim-then-send* de notification-service
(imagen `notification-service:1.2`, rama `feature/fix-duplicity`), a continuación de la topología
[1-1-1](../1-1-1/README.md). Dobla los consumidores: con `notification-request` en 6 particiones y 3 hilos
por réplica, 2 réplicas de notification = 6 hilos = las 6 particiones, el punto de escalado natural del
servicio. Lo que debe cambiar frente a 1-1-1 es el tiempo de drenaje del pipeline, no el throughput del API.

## Configuración

| Servicio | Réplicas | Imagen | Pool Hikari | Otras variables |
|---|---|---|---|---|
| document-service | 2 fijas (`min=max=2`) | `document-service:1.0` | 5 | `OUTBOX_SCHEDULER_FIXED_RATE=30000` |
| generator-service | 2 fijas | `generator-service:1.1` | 5 | `OUTBOX_SCHEDULER_FIXED_RATE=30000` |
| notification-service | 2 fijas | `notification-service:1.2` | 5 | `MAIL_PROVIDER=smtp` → Mailpit, rate limiter 100/s, `KAFKAPRODUCERCONFIG_BATCHSIZEBOOSTFACTOR=1`, compresión `lz4`, log `WARN` |
| customer-service | 1 fija | `customer-service:1.0` | 5 | |
| PostgreSQL | `Standard_B4ms` | | | 35 conexiones en uso (2×5 + 2×5 + 2×5 + 5) |
| Mailpit | 1 | `axllent/mailpit` | | `MP_MAX_MESSAGES=30000` |

Escalado con `az containerapp update --min-replicas 2 --max-replicas 2` sobre las revisiones `--0000003`, sin
cambiar imagen ni variables; quedó una única revisión activa `--0000004` por servicio. Una réplica de
notification tomó las particiones 0, 2 y 5; la otra, 1, 3 y 4.

| document | generator | notification | Mailpit |
|---|---|---|---|
| ![document 2 instancias](01-document-2-instancias.png) | ![generator 2 instancias](02-generator-2-instancias.png) | ![notification 2 instancias](03-notification-2-instancias.png) | ![Mailpit tras la prueba de 500](500/04-prueba-mailpit.png) |

## Resultados

JMeter 5.6.3 en modo CLI desde un PC en Wi-Fi contra `document-service` en Central US. Percentiles
calculados sobre el CSV de cada corrida (`elapsed` de las muestras `POST /documents`).

| Escalón | threads × loops | Duración API | Throughput | Mediana | p90 | p95 | p99 | Máx | Errores | Correos en Mailpit | Drenaje tras JMeter | Total escalón |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **500** | 10 × 50 | 31,6 s | 15,8 req/s | 140 ms | 360 ms | 458 ms | 793 ms | 1.320 ms | 0 | **500 / 500** | 48 s | **1 min 28 s** |
| **2000** | 20 × 100 | 56,3 s | 35,5 req/s | 114 ms | 189 ms | 260 ms | 389 ms | 607 ms | 0 | **2000 / 2000** | 81 s | **2 min 33 s** |
| **5000** | 25 × 200 | 95,9 s | 52,1 req/s | 112 ms | 184 ms | 262 ms | 579 ms | 1.499 ms | 0 | **5000 / 5000** | 64 s | **2 min 55 s** |

### Tiempos por tamaño

Misma definición que en 1-1-1: *JMeter* es el tiempo de pared del cliente, *drenaje* lo que tarda el pipeline
en entregar el último correo después de que JMeter termina, *total* la suma.

| Tamaño | JMeter | Drenaje | Total | Mismo tamaño en 1-1-1 |
|---|---|---|---|---|
| 500 | 40 s | 48 s | **88 s** (1 min 28 s) | 35 s + 78 s = 113 s |
| 2000 | 72 s | 81 s | **153 s** (2 min 33 s) | 110 s + 112 s = 222 s, y 2 correos nunca llegaron |
| 5000 | 111 s | 64 s | **175 s** (2 min 55 s) | 119 s + 113 s = 232 s |

Archivos por escalón, dentro de su carpeta `<total>/`: `<num>-<total>-2-2-2-<fecha>.csv` (una fila por petición, abrir en Excel),
`report/index.html` (reporte de JMeter), `results.jtl` y las capturas de esa corrida.

### Lecturas

- **Sin correos duplicados ni perdidos.** 500 correos para 500 peticiones, `SMTPRejected` en 0, health en 200.
  Con dos réplicas de notification compitiendo por el mismo topic, la carrera entre consumidores que el
  claim-then-send cierra es más probable que con una; el conteo exacto es la evidencia de que funciona.
- **El API rinde igual que con una réplica**: 15,8 req/s frente a 16,4, p95 de 458 frente a 428 ms. Con 10 hilos
  el cuello es el cliente; la segunda réplica de document-service no tiene nada que aportar a este tamaño.
- **El drenaje bajó de 78 a 48 s**, un 38 % menos, con el mismo tamaño y el mismo tick de 30 s en los
  schedulers. Es el efecto esperado de duplicar consumidores: los correos empezaron a salir en el segundo
  tick y el pipeline vació 500 en dos ticks en vez de tres. A 500 el drenaje sigue dominado por el intervalo
  del scheduler; la diferencia real se verá en 5000 y 20000.
- **2000: la corrida limpia que en 1-1-1 no se tuvo.** Allí el 2000 se lanzó con las JVM recién reiniciadas
  (p95 de 1.094 ms) y perdió dos sagas por el lock optimista tragado en document-service. Aquí, con
  contenedores calientes y dos réplicas: 35,5 req/s, p95 de 260 ms, máximo 607 ms, y **2.000 correos
  exactos**. Es el primer escalón de 2000 completo de la escalera. Drenaje de 81 s, unos 25 correos por
  segundo, con Mailpit en 30 al terminar JMeter: el pipeline arranca al ritmo de los ticks y luego sostiene
  ~35 correos/s.
- **5000: el drenaje casi se parte a la mitad.** 5.000 correos exactos, 52,1 req/s en el API (48,6 en 1-1-1,
  misma latencia: p95 262 frente a 270 ms) y drenaje de **64 s frente a 113 s**, un 43 % menos. Al terminar
  JMeter Mailpit ya tenía 1.508 correos, contra 733 en 1-1-1: con seis hilos consumiendo las seis particiones
  el pipeline avanza al doble de ritmo mientras la carga aún entra. Durante el drenaje sostuvo ~55 correos/s.
  El API apenas cambia porque a 25 hilos sigue sin saturar; la mejora es toda del pipeline, que es lo que
  esta topología debía demostrar.

  ![Mailpit tras la prueba de 2000](2000/04-prueba-mailpit.png)

## Cómo se corrió

```bash
export PATH="/c/apache-jmeter-5.6.3/apache-jmeter-5.6.3/bin:$PATH"
cd docs/jmeter/escalera
./run-escalon.sh 2-2-2 01-500-create-document.jmx
./run-escalon.sh 2-2-2 02-2000-create-document.jmx
./run-escalon.sh 2-2-2 03-5000-create-document.jmx
```

El resumen de todos los escalones de todas las topologías está en [`../escalera.csv`](../escalera.csv).
