#!/usr/bin/env bash
# Corre UN escalon de la escalera contra Azure y espera a que el pipeline drene.
#
# Uso:  ./run-escalon.sh <config> <jmx>
#   config : etiqueta de la topologia en curso, p. ej. A-1x1x1x1, B-2x2x2, C-3x3x3 (solo nombra la carpeta de resultados)
#   jmx    : uno de los planes de esta carpeta, p. ej. 01-500-create-document.jmx
#
# Ejemplo: ./run-escalon.sh A-1x1x1x1 03-5000-create-document.jmx
#
# Que hace, en orden:
#   1. vacia Mailpit (regla: cada escalon arranca con bandeja limpia)
#   2. lanza JMeter en modo CLI y genera el reporte HTML
#   3. espera hasta que Mailpit tenga == total (max 20 min), imprimiendo el avance cada 15 s
#   4. imprime SMTPAccepted/SMTPRejected y cuenta filas PENDING atascadas si hay psql y DB_URL
#
# Criterio de exito del escalon: errores 0 %, p95 < 2x el escalon anterior, correos en Mailpit == total.
set -euo pipefail

CONFIG="${1:?config (p. ej. A-1x1x1x1)}"
JMX="${2:?jmx (p. ej. 01-500-create-document.jmx)}"

HERE="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-https://document-service.redbush-a66713c9.centralus.azurecontainerapps.io}"
MP="${MP:-https://mailpit.redbush-a66713c9.centralus.azurecontainerapps.io}"
CUSTOMER_ID="${CUSTOMER_ID:-550e8400-e29b-41d4-a716-446655440001}"
export HEAP="${HEAP:--Xms1g -Xmx2g}"

# total = threads x loops, leidos de los defaults del jmx
threads=$(grep -oE '__P\(threads,[0-9]+\)' "$HERE/$JMX" | grep -oE '[0-9]+')
loops=$(grep -oE '__P\(loops,[0-9]+\)' "$HERE/$JMX" | grep -oE '[0-9]+')
total=$((threads * loops))
OUT="target/jmeter/$CONFIG/$total"

echo "=== [$CONFIG] escalon $total ($threads hilos x $loops loops) -> $OUT ==="

echo "-- vaciando Mailpit"
curl -s -X DELETE "$MP/api/v1/messages" > /dev/null
rm -rf "$OUT"; mkdir -p "$OUT"

start=$(date +%s)
jmeter -n -t "$HERE/$JMX" \
  -JbaseUrl="$BASE_URL" -JcustomerId="$CUSTOMER_ID" \
  -l "$OUT/results.jtl" -e -o "$OUT/report"
jm_end=$(date +%s)
echo "-- JMeter termino en $((jm_end - start)) s. Reporte: $OUT/report/index.html"

echo "-- esperando drenaje del pipeline (correos en Mailpit == $total, max 20 min)"
got=0
for i in $(seq 1 80); do
  got=$(curl -s "$MP/api/v1/info" | python -c 'import json,sys; print(json.load(sys.stdin)["Messages"])')
  echo "   t+$(( $(date +%s) - jm_end ))s  correos: $got / $total"
  [ "$got" -ge "$total" ] && break
  sleep 15
done
drain_end=$(date +%s)

echo "-- resumen"
curl -s "$MP/api/v1/info" | python -c 'import json,sys; d=json.load(sys.stdin); print("   Mailpit:", d["Messages"], "mensajes;", d["RuntimeStats"])'
echo "   drenaje: $((drain_end - jm_end)) s desde el fin de JMeter; total escalon: $((drain_end - start)) s"
if [ "$got" -gt "$total" ]; then echo "   !! DUPLICADOS: $((got - total)) correos de mas"; fi
if [ "$got" -lt "$total" ]; then echo "   !! FALTAN: $((total - got)) correos; revisar filas NOTIFICATION_PENDING en notification.document_outbox"; fi

if command -v psql >/dev/null && [ -n "${DB_URL:-}" ]; then
  echo "-- filas PENDING atascadas en notification.document_outbox:"
  psql "$DB_URL" -Atc "SELECT count(*) FROM notification.document_outbox WHERE notification_status='NOTIFICATION_PENDING' AND created_at < now() - interval '10 minutes';"
fi

# una linea por escalon para la tabla final
mkdir -p target/jmeter
echo "$CONFIG,$total,$threads,$loops,$((jm_end - start)),$((drain_end - jm_end)),$got" >> target/jmeter/escalera.csv
echo "-- fila agregada a target/jmeter/escalera.csv (config,total,threads,loops,seg_jmeter,seg_drenaje,correos)"
