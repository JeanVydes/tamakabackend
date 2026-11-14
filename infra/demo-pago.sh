#!/usr/bin/env bash
# Demo manual del flujo de pago, paso a paso, para ver de verdad quien dispara el webhook.
#   ./infra/demo-pago.sh
#
# La idea: la pasarela REAL es quien hace el POST a /api/pagos/webhook. Como aqui la pasarela
# es un sandbox, la dispara este script (igual que haria el proveedor desde sus servidores).
set -euo pipefail
cd "$(dirname "$0")/.."

BASE=${BASE:-http://localhost}
SECRETO=$(grep '^PAGOS_WEBHOOK_SECRET=' .env | cut -d= -f2-)
SUFIJO=$(date +%s)
CLAVE='ClaveSegura123'

api() { # api <metodo> <ruta> [token] [json]
  local metodo="$1" ruta="$2" token="${3:-}" datos="${4:-}"
  local args=(-sS -X "$metodo" "$BASE$ruta" -H 'Content-Type: application/json')
  if [[ -n "$token" ]]; then args+=(-H "Authorization: Bearer $token"); fi
  if [[ -n "$datos" ]]; then args+=(-d "$datos"); fi
  curl "${args[@]}"
}

echo "== 1. El operador registra su comercio y un servicio =="
api POST /api/cuentas "" "{\"email\":\"op$SUFIJO@demo.test\",\"password\":\"$CLAVE\",\"nombre\":\"Demo\"}" >/dev/null
TOKEN_OP=$(api POST /api/auth/login "" "{\"email\":\"op$SUFIJO@demo.test\",\"password\":\"$CLAVE\"}" | jq -r .accessToken)
TOKEN_OP=$(api POST /api/cuentas/me/modo-operador "$TOKEN_OP" | jq -r .token.accessToken)
COMERCIO=$(api POST /api/comercios "$TOKEN_OP" "{\"nombre\":\"Demo\",\"rnt\":\"RNT$SUFIJO\",\"categoria\":\"NATURALEZA\",\"descripcion\":\"demo\"}" | jq -r .id)
SERVICIO=$(api POST "/api/comercios/$COMERCIO/servicios" "$TOKEN_OP" '{"nombre":"Paseo demo","precioBase":100.00,"minPersonas":1,"maxPersonas":6}' | jq -r .id)
echo "   comercio=$COMERCIO servicio=$SERVICIO"

echo "== 2. El turista reserva y el operador acepta (monto 200.00) =="
api POST /api/cuentas "" "{\"email\":\"tur$SUFIJO@demo.test\",\"password\":\"$CLAVE\",\"nombre\":\"Demo\"}" >/dev/null
TOKEN_TUR=$(api POST /api/auth/login "" "{\"email\":\"tur$SUFIJO@demo.test\",\"password\":\"$CLAVE\"}" | jq -r .accessToken)
FECHA=$(date -d '+1 day' +%F)
RESERVA=$(api POST /api/reservas "$TOKEN_TUR" "{\"servicioId\":$SERVICIO,\"fechaReserva\":\"$FECHA\",\"numeroPersonas\":2}" | jq -r .id)
api PATCH "/api/operador/reservas/$RESERVA/respuesta" "$TOKEN_OP" '{"aceptada":true,"valorAPagar":200.00}' >/dev/null
echo "   reserva=$RESERVA"

echo "== 3. El turista inicia el pago (queda PENDIENTE) =="
# El evento ReservaAceptada viaja por el outbox: hay que esperar a que pagos tenga la reserva.
PAGO=''
for _ in $(seq 1 30); do
  PAGO=$(api POST /api/pagos "$TOKEN_TUR" "{\"reservaId\":$RESERVA}")
  [[ $(echo "$PAGO" | jq -r '.referenciaPasarela // empty') != "" ]] && break
  sleep 2
done
echo "$PAGO" | jq -c '{referenciaPasarela, monto, estado, urlPago}'
REF=$(echo "$PAGO" | jq -r .referenciaPasarela)
MONTO=$(echo "$PAGO" | jq -r .monto)

echo "== 4. Aqui lo dispara LA PASARELA (en produccion, sus servidores) =="
FIRMA=$(printf '%s' "$REF|APROBADO|$MONTO" | openssl dgst -sha256 -hmac "$SECRETO" -r | cut -d' ' -f1)
echo "   POST $BASE/api/pagos/webhook  (firma HMAC de $REF|APROBADO|$MONTO)"
curl -sS -o /dev/null -w "   respuesta: HTTP %{http_code}\n" -X POST "$BASE/api/pagos/webhook" \
  -H 'Content-Type: application/json' -H "X-Firma-Pasarela: $FIRMA" \
  -d "{\"referencia\":\"$REF\",\"estado\":\"APROBADO\",\"monto\":\"$MONTO\"}"

echo "== 5. Comprobante del turista y estado de la reserva =="
api GET "/api/pagos/mis" "$TOKEN_TUR" | jq -c '.[] | {referenciaPasarela, estado, monto}'
# La reserva se confirma cuando llega el evento PagoAprobado (outbox, relay cada ~2 s).
for _ in $(seq 1 30); do
  ESTADO=$(api GET "/api/reservas/$RESERVA" "$TOKEN_TUR" | jq -r '.estado')
  [[ "$ESTADO" == "CONFIRMADA" ]] && break
  sleep 2
done
api GET "/api/reservas/$RESERVA" "$TOKEN_TUR" | jq -c '{id, estado}'
