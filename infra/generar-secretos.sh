#!/usr/bin/env bash
# Crea backend/infra/.env con un par de claves RSA para JWT (RS256) y secretos aleatorios.
# Requiere openssl (incluido en Git Bash, Linux y macOS). No sobrescribe un .env existente.
# Uso: ./generar-secretos.sh            -> crea .env
#      ./generar-secretos.sh --claves   -> solo imprime un par de claves nuevo (para pegarlo en otro sitio)
set -euo pipefail

cd "$(dirname "$0")"
command -v openssl >/dev/null || { echo "Falta openssl" >&2; exit 1; }

temporal="$(mktemp -d)"
trap 'rm -rf "$temporal"' EXIT

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$temporal/privada.pem" 2>/dev/null
openssl pkey -in "$temporal/privada.pem" -pubout -out "$temporal/publica.pem"
# Cuerpo Base64 del PEM en una linea: PKCS#8 la privada, SPKI la publica (formato que espera JwtService).
privada="$(grep -v -- '-----' "$temporal/privada.pem" | tr -d '\r\n')"
publica="$(grep -v -- '-----' "$temporal/publica.pem" | tr -d '\r\n')"

if [[ "${1:-}" == "--claves" ]]; then
  echo "TAMAKA_JWT_PUBLIC_KEY=$publica"
  echo "TAMAKA_JWT_PRIVATE_KEY=$privada"
  exit 0
fi

if [[ -f .env ]]; then
  echo ".env ya existe: no se modifica. Para un par de claves nuevo: ./generar-secretos.sh --claves" >&2
  exit 1
fi

aleatorio() { openssl rand -hex 24; }

umask 077
sed \
  -e "s|^POSTGRES_ADMIN_PASSWORD=.*|POSTGRES_ADMIN_PASSWORD=$(aleatorio)|" \
  -e "s|^TAMAKA_DB_PASSWORD=.*|TAMAKA_DB_PASSWORD=$(aleatorio)|" \
  -e "s|^TAMAKA_JWT_PUBLIC_KEY=.*|TAMAKA_JWT_PUBLIC_KEY=$publica|" \
  -e "s|^TAMAKA_JWT_PRIVATE_KEY=.*|TAMAKA_JWT_PRIVATE_KEY=$privada|" \
  -e "s|^TAMAKA_EVENTOS_SECRETO=.*|TAMAKA_EVENTOS_SECRETO=$(aleatorio)|" \
  -e "s|^PAGOS_WEBHOOK_SECRET=.*|PAGOS_WEBHOOK_SECRET=$(aleatorio)|" \
  .env.example > .env

echo ".env creado en $(pwd) (permisos 600). Guardelo fuera del repositorio."
