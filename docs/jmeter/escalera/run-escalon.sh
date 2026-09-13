#!/usr/bin/env bash
# Corre UN escalon de la escalera contra Azure y espera a que el pipeline drene.
#
# Uso:  ./run-escalon.sh <config> <jmx>
#   config : topologia en curso = nombre de la carpeta en configuracion-servicios/: 1-1-1, 2-2-2, 3-6-6
#   jmx    : uno de los planes de esta carpeta, p. ej. 01-500-create-document.jmx
#
# Ejemplo: ./run-escalon.sh 1-1-1 03-5000-create-document.jmx
#
# Que hace, en orden:
#   1. vacia Mailpit (regla: cada escalon arranca con bandeja limpia)
#   2. lanza JMeter en modo CLI y genera el reporte HTML
#   3. espera hasta que Mailpit tenga == total (max 20 min), imprimiendo el avance cada 15 s
#   4. imprime SMTPAccepted/SMTPRejected y cuenta filas PENDING atascadas si hay psql y DB_URL
#
# Todo queda dentro del repo, en docs/jmeter/escalera/configuracion-servicios/ (junto a las capturas de cada topologia):
#   configuracion-servicios/<config>/<num>-<total>-<config>-<fecha>.csv   una fila por peticion (lo escribe el plan; abrir en Excel)
#   configuracion-servicios/<config>/<total>/results.jtl                    crudo de JMeter
#   configuracion-servicios/<config>/<total>/report/index.html              reporte HTML
#   configuracion-servicios/escalera.csv                                    una fila por escalon (resumen)
#
# Criterio de exito del escalon: errores 0 %, p95 < 2x el escalon anterior, correos en Mailpit == total.
set -euo pipefail

CONFIG="${1:?config (p. ej. 1-1-1)}"
JMX="${2:?jmx (p. ej. 01-500-create-document.jmx)}"

HERE="$(cd "$(dirname "$0")" && pwd)"
BASE_URL="${BASE_URL:-https://document-service.redbush-a66713c9.centralus.azurecontainerapps.io}"
MP="${MP:-https://mailpit.redbush-a66713c9.centralus.azurecontainerapps.io}"
CUSTOMER_ID="${CUSTOMER_ID:-550e8400-e29b-41d4-a716-446655440001}"
RESULTS_DIR="${RESULTS_DIR:-$HERE/configuracion-servicios}"
export HEAP="${HEAP:--Xms1g -Xmx2g}"

# total = threads x loops, leidos de los defaults del jmx
threads=$(grep -oE '__P\(threads,[0-9]+\)' "$HERE/$JMX" | grep -oE '[0-9]+')
loops=$(grep -oE '__P\(loops,[0-9]+\)' "$HERE/$JMX" | grep -oE '[0-9]+')
total=$((threads * loops))
OUT="$RESULTS_DIR/$CONFIG/$total"

mailpit_count() { curl -s -m 20 "$MP/api/v1/info" | python -c 'import json,sys; print(json.load(sys.stdin)["Messages"])' 2>/dev/null || echo "-1"; }

echo "=== [$CONFIG] escalon $total ($threads hilos x $loops loops) -> $OUT ==="

echo "-- vaciando Mailpit"
curl -s -X DELETE "$MP/api/v1/messages" > /dev/null
rm -rf "$OUT"; mkdir -p "$OUT"

start=$(date +%s)
jmeter -n -t "$HERE/$JMX" \
  -JbaseUrl="$BASE_URL" -JcustomerId="$CUSTOMER_ID" \
  -Jconfig="$CONFIG" -JresultsDir="$RESULTS_DIR" \
  -l "$OUT/results.jtl" -e -o "$OUT/report"
jm_end=$(date +%s)
echo "-- JMeter termino en $((jm_end - start)) s. Reporte: $OUT/report/index.html"
echo "   CSV para Excel: $(ls -t "$RESULTS_DIR/$CONFIG"/*.csv 2>/dev/null | head -1)"

echo "-- esperando drenaje del pipeline (correos en Mailpit == $total, max 20 min)"
got=0
for i in $(seq 1 80); do
  got=$(mailpit_count)
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
SUMMARY="$RESULTS_DIR/escalera.csv"
[ -f "$SUMMARY" ] || echo "config,total,threads,loops,seg_jmeter,seg_drenaje,correos,fecha,nota" > "$SUMMARY"
echo "$CONFIG,$total,$threads,$loops,$((jm_end - start)),$((drain_end - jm_end)),$got,$(date +%Y-%m-%dT%H:%M:%S),${NOTA:-}" >> "$SUMMARY"
echo "-- fila agregada a $SUMMARY"
