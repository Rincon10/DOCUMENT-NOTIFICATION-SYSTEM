# Comparativa de topologías: 1-1-1 · 2-2-2 · 3-6-6

Resultados de la escalera del 13 de septiembre de 2026 con el fix *claim-then-send* de notification-service
(`notification-service:1.2`). Tres topologías de instancias, cuatro tamaños cada una, todo contra Azure
Container Apps en Central US, Confluent Cloud y PostgreSQL `Standard_B4ms`, correo hacia Mailpit. El detalle
de cada topología, con capturas, CSV por corrida y reporte de JMeter, está en su carpeta:
[`1-1-1/`](1-1-1/README.md), [`2-2-2/`](2-2-2/README.md), [`3-6-6/`](3-6-6/README.md). Los datos crudos de
esta tabla salen de [`escalera.csv`](escalera.csv) y de los CSV de cada corrida.

## Tabla comparativa

Notación: `document · generator · notification`. *Drenaje* es lo que tarda el pipeline en entregar el último
correo después de que JMeter termina; *total* va del primer `POST` al último correo. Los percentiles son del
`elapsed` de las muestras `POST /documents`.

| Tamaño | Métrica | 1-1-1 (1·1·1) | 2-2-2 (2·2·2) | 3-6-6 (3·6·6) |
|---|---|---|---|---|
| **500** | Throughput API | 16,4 req/s | 15,8 req/s | **17,2 req/s** |
| | p95 / máx | 428 / 1.062 ms | 458 / 1.320 ms | **293** / 2.115 ms |
| | Drenaje | 78 s | **48 s** | 80 s |
| | Total | 113 s | **88 s** | 124 s |
| | Correos | 500 / 500 | 500 / 500 | 500 / 500 |
| **2000** | Throughput API | 22,3 req/s ¹ | **35,5 req/s** | 34,2 req/s |
| | p95 / máx | 1.094 / 2.155 ms ¹ | **260** / 607 ms | 317 / 2.082 ms |
| | Drenaje | 112 s (hasta 1.998) | 81 s | **64 s** |
| | Total | 222 s | 153 s | **128 s** |
| | Correos | 1.998 / 2.000 ² | 2.000 / 2.000 | 2.000 / 2.000 |
| **5000** | Throughput API | 48,6 req/s | **52,1 req/s** | 51,9 req/s |
| | p95 / máx | 270 / 1.443 ms | **262** / 1.499 ms | 323 / 1.972 ms |
| | Entregados al terminar JMeter | 733 | 1.508 | **1.833** |
| | Drenaje | 113 s | 64 s | **49 s** |
| | Total | 232 s | 175 s | **151 s** |
| | Correos | 5.000 / 5.000 | 5.000 / 5.000 | 5.000 / 5.000 |
| **20000** | Throughput API | 91,3 req/s | 94,1 req/s | **95,3 req/s** |
| | p95 / máx | 305 / 2.026 ms | 279 / 1.639 ms | **238 / 1.148 ms** |
| | Entregados al terminar JMeter | — | **9.097** | 8.860 |
| | Drenaje | bloqueado ³ | 161 s (hasta 19.998) | 175 s |
| | Total | — | 379 s (hasta 19.998) | **391 s, completo** |
| | Correos | 8.318 / 20.000 ³ | 19.998 / 20.000 ² | **20.000 / 20.000** |

¹ Corrida lanzada un minuto después de reiniciar las revisiones: JVM y pools fríos. No es comparable en
latencia con el resto.
² Dos documentos quedaron en `PENDING` por una `ObjectOptimisticLockingFailureException` tragada en
`GenerationResponseKafkaListener` de document-service; no es un problema de la topología ni del fix.
³ Prueba detenida a los 12 minutos: el productor Kafka de la única réplica de document-service perdió
brokers de Confluent y expiró lotes; el pipeline no avanzaba.

### Referencia: la misma topología 3-6-6 el 9 de septiembre, sin el fix

| | 9 sep, `notification-service:1.0` | 13 sep, `notification-service:1.2` |
|---|---|---|
| Peticiones OK | 20.000 / 20.000 | 20.000 / 20.000 |
| Throughput API | 97 req/s | 95,3 req/s |
| p95 / máx | 312 / 2.252 ms | 238 / 1.148 ms |
| **Correos en Mailpit** | **22.278** (+2.278, 11,4 %) | **20.000** |

## Análisis

**1. El API no distingue topologías; el pipeline sí.** El throughput de `POST /documents` está gobernado por
los hilos de JMeter, no por las réplicas de document-service: 16-17 req/s con 10 hilos, 34-35 con 20, 52 con
25 y 91-95 con 40, casi idénticos en las tres topologías. La latencia tampoco se mueve de forma
consistente: 3-6-6 tiene el mejor p95 en 500 y 20000, 2-2-2 en 2000 y 5000, y las diferencias son de
decenas de milisegundos sobre un enlace Wi-Fi hasta Central US. Lo que sí separa las topologías es lo que
pasa después del `200`: cuántos correos ya salieron cuando JMeter termina y cuánto tarda en salir el último.

**2. De una a dos réplicas la ganancia es grande; de dos a 3-6-6 es más pequeña pero real.** En el
tamaño donde las tres son comparables sin asteriscos, 5000, el drenaje pasa de 113 s a 64 s (−43 %) al
doblar, y de 64 s a 49 s (−23 %) al pasar a 3-6-6; el total baja de 232 s a 175 s y a 151 s. El primer salto
lo explica notification: con 6 particiones y 3 hilos por réplica, dos réplicas cubren exactamente las 6
particiones. El segundo lo explica el generador: con 6 réplicas la generación deja de acumular cola y las
solicitudes de notificación llegan antes. Se ve en los correos ya entregados cuando JMeter termina: 733,
1.508 y 1.833. Las 12 hilos sobrantes de notification en 3-6-6 no aportan, y por eso la curva se aplana.

**3. A 500 el drenaje no mide capacidad.** Los tres valores (78, 48, 80 s) están dictados por el tick de
30 s de los schedulers de outbox y por en qué punto del tick cae el fin de JMeter, no por las réplicas. En
3-6-6 el primer correo tardó 17 s y 401 de 500 llegaron de golpe en el segundo tick. Con 500 peticiones no
hay cola que drenar, solo esperas de scheduler. Por eso la escalera lo usa como calentamiento y línea base
de latencia, no como comparación de pipeline.

**4. A 20000 la topología decide si la prueba termina.** Con una réplica de document-service, un único
productor Kafka concentró todas las conexiones a Confluent, perdió brokers y expiró lotes: el pipeline se
quedó en 8.318 y hubo que detener la prueba. Con dos réplicas Kafka aguantó y llegaron 19.998. Con 3-6-6
llegaron los 20.000. El drenaje de 2-2-2 parece 14 s más corto que el de 3-6-6, pero es la cifra hasta
19.998: los dos correos restantes nunca llegaron, así que no hay un tiempo total de 2-2-2 con el que
comparar. 3-6-6 es la única topología con un 20000 completo, y lo hizo con el mejor p95 (238 ms) y el
mejor máximo (1,1 s) de toda la escalera.

**5. Cero duplicados en 12 corridas, 62.500 peticiones.** Es el resultado del fix. La prueba más exigente
es la última fila: misma topología, misma BD y mismo plan que el 9 de septiembre, cuando llegaron 2.278
correos de más; hoy llegaron exactamente 20.000. Las réplicas compitiendo por las mismas particiones, los
rebalances de arranque y las republicaciones del outbox siguen ahí (el generador rechazó cientos de
solicitudes duplicadas en cada corrida grande), pero ninguna se convirtió en un segundo correo: el claim en
`document_outbox` antes de enviar decide quién envía y descarta al resto.

**6. Lo que sigue fallando no depende de las réplicas.** Los dos documentos sin correo (uno en 1-1-1/2000
y dos en 2-2-2/20000, ninguno en 3-6-6) se deben a que `GenerationResponseKafkaListener` traga la
excepción de bloqueo optimista y commitea el offset, dejando la saga sin completar. Aparece cuando Kafka
parpadea con backlog grande y el scheduler del outbox republica; es independiente de la topología y se
corrige relanzando la excepción para que Kafka reentregue. Hasta entonces, la consulta de recuperación
está en los README de 1-1-1 y 2-2-2.

## Qué se probó en cada topología: disponibilidad y rendimiento

Cada topología se sometió a los mismos cuatro tamaños de carga (500, 2000, 5000 y 20000 peticiones) con el
mismo plan de JMeter, y de paso a los eventos de infraestructura que ocurrieron ese día. Lo que sigue
separa lo que se observó de **disponibilidad** (¿el sistema siguió atendiendo y entregando cuando algo
cambió o falló?) de lo que se midió de **rendimiento** (throughput, latencia, drenaje).

### 1-1-1: una instancia por servicio

**Disponibilidad.** Es la topología sin redundancia y lo mostró en cuatro eventos:

- El cambio de SKU de la base (`B1ms` → `B4ms`) dejó los tres servicios sin conexiones durante unos minutos
  (`HikariPool - Connection is not available`, `Connect timed out`): `/actuator/health` de document-service
  respondió 503 y los schedulers de outbox fallaron hasta que Postgres volvió a Ready. Con una réplica no hay
  quien absorba el corte; el sistema se recuperó solo al volver la BD, sin intervención ni datos perdidos.
- A 20000 peticiones, el único productor Kafka de document-service perdió brokers de Confluent
  (`socket connection setup timeout`) y expiró lotes: el pipeline se quedó en 8.318 correos y hubo que
  detener la prueba. Las 11.700 sagas restantes quedaron en `STARTED` en el outbox, recuperables, pero el
  servicio no se recuperó por sí mismo en 12 minutos.
- Reiniciar las revisiones (`az containerapp revision restart`, sin cambiar nada) devolvió la conectividad
  con Kafka. Es la mitigación documentada y funcionó, a costa de arrancar con JVM frías.
- En el 2000 posterior al reinicio, un documento de cada mil quedó sin correo por la excepción de bloqueo
  optimista tragada en document-service. El API respondió 200 a todo: la pérdida fue invisible para el
  cliente y solo la detectó el conteo de Mailpit.

**Rendimiento.** 16-49 req/s según los hilos, p95 entre 270 y 430 ms con contenedores calientes, drenaje de
78 s (500) y 113 s (5000), unos 40 correos/s con tres hilos de notification. Suficiente hasta 5000; a 20000 el
cuello no fue CPU ni BD sino la conexión productor → Kafka de una sola réplica bajo backlog.

### 2-2-2: dos instancias por servicio

**Disponibilidad.** El escalado se hizo con `az containerapp update` en caliente: cada servicio creó la
revisión `--0000004`, las dos réplicas nuevas arrancaron y la vieja se desaprovisionó sin cortar el API. El
consumer group de notification se rebalanceó y repartió las 6 particiones en 3 + 3, cubriéndolas todas. En el
20000 los tres servicios perdieron un broker cada uno durante 90 segundos; con dos productores por servicio
Kafka aguantó sin expirar lotes y el pipeline no se detuvo. Faltaron dos correos por el mismo defecto de
código del listener, independiente de las réplicas.

**Rendimiento.** Igual throughput de API que 1-1-1 (el límite es el cliente) y drenaje a la mitad: 48 s
(500), 81 s (2000), 64 s (5000), 161 s hasta 19.998 (20000). Al terminar JMeter ya había el doble de
correos entregados que en 1-1-1. La mejora la explica notification: seis hilos para seis particiones.

### 3-6-6: document 3 · generator 6 · notification 6

**Disponibilidad.** Escalado en caliente a 15 réplicas sin corte. Durante el rebalance de arranque el
generador rechazó 278 solicitudes duplicadas con `23505`: la guarda de idempotencia absorbió las
reentregas sin efectos. No hubo errores de pool (80 conexiones sobre `B4ms`), ni expiraciones Kafka, ni
sagas perdidas en ninguna corrida. Es la única topología que completó el 20000 y la única sin ningún
asterisco en la tabla. 12 de los 18 hilos de notification quedaron ociosos: más réplicas de notification
no añaden disponibilidad ni capacidad mientras el topic tenga 6 particiones.

**Rendimiento.** Mejor p95 en 500 (293 ms) y 20000 (238 ms), mejor drenaje en 2000 (64 s) y 5000 (49 s), y
20.000 correos en 6 min 31 s. Frente a la misma topología del 9 de septiembre: mismo API, 2.278 correos de
más menos.

### Conclusión de disponibilidad y rendimiento

- **Rendimiento**: el API rinde igual en las tres topologías (91-95 req/s a 40 hilos); escalar réplicas de
  document-service no sube el throughput porque el límite es el cliente. Lo que escala es el pipeline
  asíncrono: pasar de 1 a 2 réplicas recorta el drenaje un 43 % y pasar a 3-6-6 otro 23 %, con la ganancia
  concentrada en notification hasta cubrir las 6 particiones y luego en generator.
- **Disponibilidad**: con una réplica cada evento externo (cambio de BD, broker caído) se convierte en
  indisponibilidad del pipeline y a veces requiere intervención manual; con dos o más, los mismos eventos
  se absorben sin detener la entrega. Los rollouts de Container Apps no cortaron el API en ninguna
  topología. El único fallo que ninguna topología evita es el defecto de código del listener de
  document-service, porque no depende de capacidad sino de cómo se maneja una excepción.
- **Correctitud bajo carga**: 62.500 peticiones, 0 correos duplicados, 3 documentos sin correo por un
  defecto conocido y recuperable. El fix cumple su objetivo en las tres topologías y en el escenario de
  referencia de 20.000.
- **Recomendación**: 2-2-2 como mínimo operativo (redundancia y la mayor parte de la ganancia), 3-6-6 o
  equivalente con notification en 2 para picos de 20.000, y corregir el listener antes de dar por cerrada
  la disponibilidad del pipeline.

## Conclusión

- **El fix funciona bajo carga real**: 62.500 peticiones en tres topologías sin un solo correo duplicado, y
  20.000 exactos en el escenario que antes producía 22.278. El conteo de Mailpit vuelve a ser una medida de
  éxito válida, que era el criterio de aceptación pendiente.
- **3-6-6 es la topología recomendada para volumen**: única con el 20000 completo, mejor drenaje en 2000 y
  5000, mejor latencia en el escalón grande, y sin pérdidas. Su coste frente a 2-2-2 son 9 réplicas más,
  de las cuales 4 de notification están ociosas: con 6 particiones, notification no necesita más de 2
  réplicas; el escalado útil está en generator y document.
- **2-2-2 es el punto de equilibrio**: captura la mayor parte de la mejora (−43 % de drenaje frente a
  1-1-1) con la mitad de réplicas, y a 20000 ya no se bloquea. Le faltó cerrar dos sagas por un defecto
  de código, no de capacidad.
- **1-1-1 no sirve para 20000**: un solo productor Kafka es el punto único de fallo del pipeline cuando
  hay backlog. Es válida para 5000 o menos.
- **Pendiente de código, en orden**: relanzar la excepción de bloqueo optimista en los listeners de
  document-service; estado "en vuelo" en los schedulers de outbox para no republicar lo pendiente de ack;
  y bajar `OUTBOX_SCHEDULER_FIXED_RATE` de 30 s si se quiere acortar el drenaje en tamaños pequeños, hoy
  dominado por el tick.
