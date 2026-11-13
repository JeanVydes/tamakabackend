-- Idempotencia de los consumidores: un mismo hecho (tipo + reserva) se registra una sola vez,
-- aunque el bus reentregue el evento.
-- Esta migracion nunca se publico antes de esta version (no esta en el historial git), por eso se corrige aqui
-- en lugar de crear una V3.

-- 1. Elimina duplicados previos, conservando el registro de menor id (el primero que llego).
DELETE FROM registro_demanda r
USING registro_demanda d
WHERE r.tipo = d.tipo
  AND r.referencia_id = d.referencia_id
  AND r.id > d.id;

-- 2. Con la tabla ya limpia, el indice unico se puede crear sin fallar.
CREATE UNIQUE INDEX uk_demanda_tipo_referencia ON registro_demanda (tipo, referencia_id);
