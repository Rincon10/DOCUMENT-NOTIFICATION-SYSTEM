# Análisis de disponibilidad y eficiencia: escalera de topologías

Análisis comparativo de dos atributos de calidad, **disponibilidad** y **eficiencia de desempeño**, a partir
de la escalera de pruebas del 13 de septiembre de 2026 sobre tres topologías de instancias en Azure
Container Apps: `1-1-1`, `2-2-2` y `3-6-6` (document · generator · notification). Los datos salen de las
doce corridas documentadas en
[`docs/jmeter/escalera/configuracion-servicios/`](jmeter/escalera/configuracion-servicios/README.md); aquí
no se repiten las tablas completas, se interpretan.

El objetivo es responder tres preguntas en lenguaje llano pero con evidencia:

1. ¿Qué tan disponible es el sistema en cada topología, y qué lo tumba?
2. ¿Qué tan eficiente es, y dónde está el cuello de botella real?
3. ¿Cuántas réplicas hacen falta y dónde?

## 1. Qué medimos y cómo

### Los dos atributos, en corto

| Atributo | Pregunta que responde | Cómo lo observamos en la escalera |
|---|---|---|
| **Disponibilidad** | ¿El sistema sigue aceptando peticiones y entregando correos cuando algo cambia o falla? | Errores HTTP, health checks, si el pipeline se detiene, si el sistema se recupera solo o necesita intervención, y si cada documento aceptado termina en un correo (**completitud**: correos en Mailpit frente a peticiones) |
| **Eficiencia de desempeño** | ¿Cuánto trabajo hace por unidad de tiempo y con cuánto retraso? | Throughput del API (req/s), latencia (p95, máximo), **tiempo de drenaje** del pipeline y **utilización** de las réplicas |

Un matiz importante sobre disponibilidad: no hicimos pruebas de caos deliberadas (matar réplicas, cortar
la red a propósito). Lo que se analiza es la disponibilidad **observada** ante los eventos reales que
ocurrieron durante el día, que fueron varios y útiles: un cambio de SKU de la base de datos, pérdidas de
conectividad con brokers de Kafka, reinicios de revisiones y rollouts de escalado.

### La arquitectura, en una frase

Un `POST /documents` responde `200` en cuanto document-service guarda el documento y una fila de *outbox*.
Todo lo demás es asíncrono: un scheduler publica a Kafka, generator-service genera el documento y responde,
document-service procesa la respuesta y publica una solicitud de notificación, notification-service envía el
correo. Por eso **el API y el pipeline se miden por separado**: el API puede estar perfecto mientras el
pipeline está detenido, y eso fue exactamente lo que pasó en una de las corridas.

### Las topologías

| Topología | document | generator | notification | Hilos de notification vs. particiones del topic |
|---|---|---|---|---|
| `1-1-1` | 1 | 1 | 1 | 3 hilos para 6 particiones: la mitad se atiende en serie |
| `2-2-2` | 2 | 2 | 2 | 6 hilos para 6 particiones: cobertura exacta |
| `3-6-6` | 3 | 6 | 6 | 18 hilos para 6 particiones: 12 ociosos |

Ese dato de particiones explica casi todo lo que sigue.

## 2. Disponibilidad

### 2.1 Qué pasó en cada topología

| Evento | `1-1-1` | `2-2-2` | `3-6-6` |
|---|---|---|---|
| Rollout de escalado (nueva revisión) | — | Sin corte del API; rebalance de Kafka en segundos | Sin corte del API; 278 reentregas absorbidas por la guarda de idempotencia del generador |
| Cambio de SKU de la BD (`B1ms` → `B4ms`) | Pipeline y health caídos varios minutos; se recuperó solo al volver Postgres | no ocurrió | no ocurrió |
| Pérdida de brokers de Confluent con backlog grande | **Pipeline detenido** en 8.318 de 20.000; requirió reiniciar revisiones | Un broker perdido por servicio durante 90 s; el pipeline **siguió** | Sin incidentes |
| Completitud (correos entregados / peticiones) | 500/500, 5000/5000, 1.998/2.000, 8.318/20.000 | 500, 2000, 5000 completos; 19.998/20.000 | **Todos completos**, incluido 20.000/20.000 |
| Correos duplicados | 0 | 0 | 0 |

### 2.2 Lectura

**Con una réplica no hay redundancia, y cada evento externo se vuelve indisponibilidad del pipeline.** El
API de document-service siguió respondiendo `200` durante todo el día, incluso cuando la base de datos estaba
reiniciando (la petición se acepta, el trabajo se encola). Pero el pipeline es otra historia: el único
productor Kafka de document-service concentraba todas las conexiones hacia Confluent, y cuando perdió
brokers no hubo otra réplica que siguiera publicando. El sistema no se recuperó solo en 12 minutos y hubo
que reiniciar revisiones a mano. Ese es el patrón clásico del **punto único de fallo**.

**Con dos réplicas el mismo evento se absorbe.** En el 20000 de `2-2-2` los tres servicios perdieron un
broker cada uno durante 90 segundos. Nadie se enteró desde fuera: el otro productor siguió publicando, no
expiró ningún lote y el drenaje continuó. Aquí la redundancia hizo lo que promete.

**El outbox garantiza que no se pierde trabajo, pero no que se termine a tiempo.** En el bloqueo de `1-1-1`
las 11.700 sagas pendientes quedaron a salvo en `STARTED` en la base de datos: al recuperar la conexión, el
scheduler las habría publicado. La disponibilidad de los datos fue total; la del servicio, no. Conviene
distinguir ambas cosas al hablar de "disponibilidad".

**Los rollouts no cuestan disponibilidad.** Container Apps crea la revisión nueva, espera a que sus réplicas
estén sanas y desaprovisiona la vieja. Escalamos de 1 a 2 y de 2 a 15 réplicas con carga cero, sin un solo
error, y el consumer group de Kafka se rebalanceó en segundos. El precio es que las JVM nuevas están frías:
la corrida de 2000 lanzada un minuto después de un reinicio tuvo el p95 más alto de la escalera (1.094 ms).

**Los duplicados desaparecieron en las tres topologías.** El fix *claim-then-send* de notification-service
se probó en el peor caso posible para él: réplicas compitiendo por las mismas particiones, rebalances de
arranque y republicaciones del outbox. En 62.500 peticiones llegaron exactamente los correos esperados,
salvo los tres del siguiente punto. Como referencia, la misma topología `3-6-6` el 9 de septiembre, sin el
fix, entregó 2.278 correos de más.

**Hay un defecto que ninguna topología evita.** Tres documentos en todo el día (uno en `1-1-1`, dos en
`2-2-2`) quedaron en `PENDING` sin correo. La causa es de código: cuando el scheduler del outbox y la saga
que procesa la respuesta del generador actualizan la misma fila a la vez, salta una excepción de bloqueo
optimista que el listener de document-service captura, registra y descarta, confirmando el offset de Kafka.
El API dijo `200`, el pipeline lo perdió, y solo el conteo de Mailpit lo delató. Es una **pérdida silenciosa**,
el tipo de fallo de disponibilidad más caro porque no dispara ninguna alarma. No depende de réplicas: se
corrige relanzando la excepción para que Kafka reentregue el mensaje.

### 2.3 Tácticas de disponibilidad presentes y ausentes

| Táctica | ¿Está? | Evidencia |
|---|---|---|
| Redundancia activa (varias réplicas) | Sí, configurable | `2-2-2` absorbió la pérdida de brokers que bloqueó a `1-1-1` |
| Cola persistente entre etapas (outbox + Kafka) | Sí | El trabajo pendiente sobrevivió al bloqueo y al reinicio |
| Idempotencia en consumidores | Sí | 278 y miles de reentregas rechazadas con `23505`; 0 correos duplicados |
| Reintento con reentrega de Kafka | **Parcial** | Funciona para excepciones no capturadas; el listener de respuestas de generación captura la de bloqueo optimista y la anula |
| Health checks que reflejan dependencias | Sí | `/actuator/health` bajó a 503 cuando la BD no respondía |
| Recuperación automática ante pérdida de conectividad Kafka | **No** con una réplica | El productor no reconectó por sí mismo en 12 minutos |
| Detección de pérdida silenciosa | **No** | Solo el conteo externo de Mailpit la reveló |

## 3. Eficiencia de desempeño

### 3.1 Throughput del API: no depende de la topología

| Hilos de JMeter | `1-1-1` | `2-2-2` | `3-6-6` |
|---|---|---|---|
| 10 (500 peticiones) | 16,4 req/s | 15,8 | 17,2 |
| 20 (2000) | 22,3 ¹ | 35,5 | 34,2 |
| 25 (5000) | 48,6 | 52,1 | 51,9 |
| 40 (20000) | 91,3 | 94,1 | 95,3 |

¹ JVM frías tras un reinicio.

El throughput sube con los hilos del cliente y casi no con las réplicas. Con 40 hilos y una latencia media
de unos 140 ms, 40 peticiones en vuelo dan unas 285 req/s teóricas; el cliente en Wi-Fi hasta Central US
alcanza 91-95. **El cuello del API es el cliente de prueba, no el servidor.** Añadir réplicas de
document-service para subir req/s sería gastar sin medir nada nuevo; habría que subir hilos o lanzar desde
la misma región.

### 3.2 Latencia: estable, con ruido

El p95 se movió entre 238 y 458 ms en todas las corridas calientes, sin una tendencia clara por topología:
`3-6-6` tuvo el mejor en 500 (293 ms) y 20000 (238 ms), `2-2-2` en 2000 (260 ms) y 5000 (262 ms). Las
diferencias son de decenas de milisegundos sobre un enlace con jitter propio. Lo relevante es lo que **no**
pasó: la latencia no se degradó al subir de 500 a 20.000 peticiones en ninguna topología. El API no se
satura en este rango.

### 3.3 Drenaje del pipeline: aquí sí manda la topología

| Tamaño | `1-1-1` | `2-2-2` | `3-6-6` | Mejora 1→2 | Mejora 2→3-6-6 |
|---|---|---|---|---|---|
| 500 | 78 s | 48 s | 80 s | dominado por el tick del scheduler | — |
| 2000 | 112 s (1.998) | 81 s | **64 s** | −28 % | −21 % |
| 5000 | 113 s | 64 s | **49 s** | **−43 %** | **−23 %** |
| 20000 | bloqueado | 161 s (19.998) | 175 s (completo) | de no terminar a terminar | comparación no aplica |

Y los correos ya entregados en el instante en que JMeter termina, que miden cuánto avanza el pipeline
mientras la carga todavía entra:

| Tamaño | `1-1-1` | `2-2-2` | `3-6-6` |
|---|---|---|---|
| 5000 | 733 | 1.508 | 1.833 |
| 20000 | — | 9.097 | 8.860 |

Tres lecturas:

- **El primer salto es de notification.** De 3 a 6 hilos consumidores se cubren las 6 particiones del topic;
  el drenaje a 5000 cae un 43 %. Es la mejora más barata de la escalera: una réplica más.
- **El segundo salto es del generador.** `3-6-6` no añade nada útil a notification (12 hilos ociosos), pero
  seis réplicas de generator hacen que la generación deje de acumular cola, y las solicitudes de notificación
  llegan antes. Se ve en el 5000: 1.833 correos ya entregados frente a 1.508.
- **A 500 el drenaje no mide capacidad.** Los schedulers del outbox corren cada 30 segundos; con 500
  peticiones no hay cola, solo espera al siguiente tick. Los 78, 48 y 80 s dependen de dónde cae el fin de
  JMeter respecto al tick. Por eso el 500 es calentamiento, no comparación.

### 3.4 Utilización: cuánto de lo desplegado trabaja

| Topología | Réplicas totales | Hilos de notification útiles / desplegados | Pool BD en uso | Comentario |
|---|---|---|---|---|
| `1-1-1` | 4 | 3 / 3 | 20 conexiones | Todo trabaja, pero es punto único de fallo |
| `2-2-2` | 7 | 6 / 6 | 35 | Sin desperdicio; cobertura exacta de particiones |
| `3-6-6` | 16 | 6 / 18 | 80 | 4 réplicas de notification (12 hilos) no aportan; generator y document sí |

La eficiencia en el sentido de "recursos que producen resultado" es máxima en `2-2-2`. `3-6-6` compra un
20-25 % más de velocidad de drenaje y la única corrida completa de 20.000 con más del doble de réplicas, de
las cuales cuatro están de sobra. Una topología `3-6-2` habría rendido igual con 12 réplicas.

## 4. Comparación resumida

Escala: ● bueno · ◐ aceptable con reservas · ○ insuficiente. Es un juicio a partir de las corridas, no una
métrica.

| Criterio | `1-1-1` | `2-2-2` | `3-6-6` |
|---|---|---|---|
| Disponibilidad del API bajo carga | ● | ● | ● |
| Disponibilidad del pipeline ante fallos externos | ○ se detuvo, requirió intervención | ● absorbió la pérdida de brokers | ● sin incidentes |
| Completitud (sin pérdidas) | ◐ 1 de 2.000 y bloqueo a 20.000 | ◐ 2 de 20.000 | ● 27.500 de 27.500 |
| Sin duplicados | ● | ● | ● |
| Throughput del API | ● | ● | ● |
| Latencia | ● | ● | ● |
| Drenaje del pipeline | ○ | ● | ● el mejor |
| Utilización de recursos | ● | ● | ◐ 12 hilos ociosos |
| Coste (réplicas) | 4 | 7 | 16 |

## 5. Conclusiones

1. **El API no es el problema y no necesita réplicas para el rendimiento.** Respondió `200` a 62.500
   peticiones sin un error, con latencia estable, y su throughput lo fija el cliente de prueba. Las réplicas
   de document-service se justifican por disponibilidad del pipeline (varios productores Kafka), no por
   req/s.

2. **La disponibilidad real del sistema es la del pipeline, y esa depende de la redundancia.** Una réplica
   convierte cada parpadeo externo en una parada que exige intervención; dos réplicas absorbieron el mismo
   parpadeo sin que nadie lo notara. `2-2-2` es el mínimo operativo razonable.

3. **La eficiencia del pipeline escala por etapas y se satura por particiones.** Notification rinde hasta
   cubrir las 6 particiones (2 réplicas) y luego no mejora; después la ganancia viene del generador. Para
   seguir escalando notification habría que aumentar particiones del topic, no réplicas.

4. **`3-6-6` es la topología más sólida pero no la más eficiente.** Única con el 20000 completo y sin
   pérdidas, mejor drenaje en 2000 y 5000, mejor latencia en el escalón grande. Paga con cuatro réplicas de
   notification ociosas. Para picos de 20.000, `3-6-2` daría lo mismo con menos.

5. **Dos defectos de código pesan más que cualquier topología.** El listener que anula la excepción de
   bloqueo optimista produce pérdidas silenciosas independientes de las réplicas, y el scheduler del outbox
   que republica todo lo pendiente de ack cada 30 s crea la contención que dispara esas excepciones y
   alimenta las tormentas hacia Kafka. Corregir ambos mejora disponibilidad y eficiencia a la vez, a coste
   cero en infraestructura.

6. **El fix de duplicados cumplió.** Cero correos duplicados en doce corridas y tres topologías, incluida la
   réplica exacta del escenario que producía 2.278 de más. El conteo de la bandeja vuelve a ser una métrica
   de aceptación válida.

### Recomendaciones, en orden

| Prioridad | Acción | Atributo que mejora | Coste |
|---|---|---|---|
| 1 | Relanzar `OptimisticLockingFailureException` en los listeners de document-service para que Kafka reentregue | Disponibilidad (elimina pérdidas silenciosas) | Código, bajo |
| 2 | Estado "en vuelo" en los schedulers de outbox para no republicar filas pendientes de ack | Disponibilidad y eficiencia (menos contención y menos tormentas Kafka) | Código, medio |
| 3 | Operar con `2-2-2` como base; `3-6-2` para picos | Disponibilidad con utilización alta | Infraestructura, moderado |
| 4 | Alerta sobre documentos en `PENDING` con outbox `COMPLETED` más antiguos que N minutos | Detección de pérdida silenciosa | Consulta + monitor, bajo |
| 5 | Bajar el tick del scheduler (`OUTBOX_SCHEDULER_FIXED_RATE`) si importa el drenaje en tamaños pequeños | Eficiencia en volúmenes bajos | Configuración, trivial; solo después de la acción 2 |

## Anexo: fuentes

- Plan y método: [`docs/jmeter/escalera/README.md`](jmeter/escalera/README.md)
- Tabla comparativa completa, análisis por corrida y conclusión: [`configuracion-servicios/README.md`](jmeter/escalera/configuracion-servicios/README.md)
- Detalle por topología, con capturas de Azure y Mailpit, CSV por corrida y reportes de JMeter:
  [`1-1-1`](jmeter/escalera/configuracion-servicios/1-1-1/README.md) ·
  [`2-2-2`](jmeter/escalera/configuracion-servicios/2-2-2/README.md) ·
  [`3-6-6`](jmeter/escalera/configuracion-servicios/3-6-6/README.md)
- Resumen crudo, una fila por corrida: [`escalera.csv`](jmeter/escalera/configuracion-servicios/escalera.csv)
- Mecanismos contra la duplicidad y el fix *claim-then-send*: [README principal](../README.md#mecanismos-contra-la-duplicidad)
