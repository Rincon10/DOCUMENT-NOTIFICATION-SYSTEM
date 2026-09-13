# Topología 1-1-1: una instancia por servicio

Corrida del 13 de septiembre de 2026 con el fix *claim-then-send* de notification-service
(imagen `notification-service:1.2`, rama `feature/fix-duplicity`). Es la línea base de la escalera:
mide el techo de una sola réplica por servicio y la velocidad del pipeline con un solo consumidor por topic.

## Configuración

| Servicio | Réplicas | Imagen | Pool Hikari | Otras variables |
|---|---|---|---|---|
| document-service | 1 fija (`min=max=1`) | `document-service:1.0` | 5 | `OUTBOX_SCHEDULER_FIXED_RATE=30000` |
| generator-service | 1 fija | `generator-service:1.1` | 5 | `OUTBOX_SCHEDULER_FIXED_RATE=30000` |
| notification-service | 1 fija | `notification-service:1.2` | 5 | `MAIL_PROVIDER=smtp` → Mailpit, rate limiter 100/s, `KAFKAPRODUCERCONFIG_BATCHSIZEBOOSTFACTOR=1`, compresión `lz4`, log `WARN` |
| customer-service | 1 fija | `customer-service:1.0` | 5 | |
| PostgreSQL | `Standard_B4ms` | | | subida desde `B1ms` justo antes de la corrida |
| Mailpit | 1 | `axllent/mailpit` | | `MP_MAX_MESSAGES=30000` |

Kafka en Confluent Cloud; `notification-request` tiene 6 particiones y cada réplica de notification levanta
3 hilos consumidores, así que con una réplica la mitad de las particiones se atienden en serie.

| document | generator | notification | Mailpit |
|---|---|---|---|
| ![document 1 instancia](01-document-1-instancia.png) | ![generator 1 instancia](02-generator-1-instancia.png) | ![notification 1 instancia](03-notification-1-instancia.png) | ![Mailpit tras la prueba de 500](500/04-prueba-mailpit.png) |

## Resultados

JMeter 5.6.3 en modo CLI desde un PC en Wi-Fi contra `document-service` en Central US. Percentiles
calculados sobre el CSV de cada corrida (`elapsed` de las muestras `POST /documents`).

| Escalón | threads × loops | Duración API | Throughput | Mediana | p90 | p95 | p99 | Máx | Errores | Correos en Mailpit | Drenaje tras JMeter |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **500** | 10 × 50 | 30,5 s | 16,4 req/s | 224 ms | 371 ms | 428 ms | 740 ms | 1.062 ms | 0 | **500 / 500** | 78 s |
| **5000** | 25 × 200 | 103 s | 48,6 req/s | 119 ms | 219 ms | 270 ms | 588 ms | 1.443 ms | 0 | **5000 / 5000** | 113 s |
| 20000, intento 1 | 40 × 500 | 557 s | — | 122 ms | — | 279 ms | 480 ms | 1.529 ms | 7.718 (38,6 %) | 12.293 / 12.282 llegadas | — |

Archivos por escalón, dentro de su carpeta `<total>/`: `<num>-<total>-1-1-1-<fecha>.csv` (una fila por petición, abrir en Excel),
`report/index.html` (reporte de JMeter), `results.jtl` y las capturas de esa corrida.

### Lecturas

- **Sin correos duplicados.** 500 correos para 500 peticiones y 5.000 para 5.000, `SMTPRejected` en 0. En la
  corrida del 9 de septiembre, sin el fix, el escalón de 9.000 ya dejaba 49 correos de más. El claim previo al
  envío (fila de outbox en `NOTIFICATION_PENDING` protegida por el índice único) está haciendo su trabajo.
- **El API no satura con una réplica** a estos tamaños. Igual que en septiembre, la latencia baja al subir de
  10 a 25 hilos porque los contenedores ya están calientes: p95 de 428 a 270 ms. A 40 hilos las muestras que sí
  llegaron al servidor mantuvieron p95 de 279 ms.
- **El pipeline con un solo consumidor drena a unas 40 notificaciones por segundo.** Con 5.000 peticiones,
  Mailpit ya tenía 733 correos al terminar JMeter y completó los 5.000 en 113 s más. Ese número es el que
  debe bajar en las topologías 2-2-2 y 3-6-6; el throughput del API no.
- **El health inicial devolvía 500** porque el Header Manager global del plan manda
  `Accept: application/vnd.api.v1+json`, que Actuator no produce. Los planes de la escalera llevan un
  `Accept: application/json` propio en ese sampler; a partir del escalón de 5.000 responde 200.

### Intento 1 de 20.000: inválido por la red del cliente

El PC que corría JMeter perdió la red entre el segundo 60 y el 450 de la prueba: durante seis minutos y medio
no salió ni una muestra, y al volver la conexión 7.705 peticiones fallaron con `UnknownHostException` (DNS)
y 13 con `Connection reset`. Solo 12.282 peticiones recibieron 200. Mailpit terminó en 12.293: las 12.282
exitosas más 11 de las 13 que el servidor sí procesó aunque el cliente viera el reset. No hay duplicados,
pero el escalón no mide el sistema, así que se repite. Los archivos quedan en
`20000-intento1-caida-red-cliente/` para referencia.

## Cómo se corrió

```bash
export PATH="/c/apache-jmeter-5.6.3/apache-jmeter-5.6.3/bin:$PATH"
cd docs/jmeter/escalera
./run-escalon.sh 1-1-1 01-500-create-document.jmx
./run-escalon.sh 1-1-1 03-5000-create-document.jmx
./run-escalon.sh 1-1-1 04-20000-create-document.jmx
```

El resumen de todos los escalones de todas las topologías está en [`../escalera.csv`](../escalera.csv).
