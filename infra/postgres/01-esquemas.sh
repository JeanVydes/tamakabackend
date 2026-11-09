#!/bin/sh
# Crea, por servicio, un esquema propio y un usuario con permisos solo sobre ese esquema
# (regla de datos: ningun servicio accede a tablas de otro).
set -e

for servicio in cuentas comercios atractivos eventos reservas pagos indicadores; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
      IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'tamaka_${servicio}') THEN
        CREATE ROLE tamaka_${servicio} LOGIN PASSWORD '${TAMAKA_DB_PASSWORD}';
      END IF;
    END
    \$\$;
    CREATE SCHEMA IF NOT EXISTS ${servicio} AUTHORIZATION tamaka_${servicio};
EOSQL
done
