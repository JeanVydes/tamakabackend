-- El operador dueno del servicio llega en el evento ReservaAceptada. Se guarda en la proyeccion y en el pago
-- para que el operador consulte sus cobros (GET /api/operador/pagos) y para enriquecer PagoAprobado.
-- Nullable: las filas creadas antes de esta migracion no lo tienen.
ALTER TABLE reserva_pagable ADD COLUMN operador_id BIGINT;
ALTER TABLE pago ADD COLUMN operador_id BIGINT;

CREATE INDEX idx_pago_operador ON pago (operador_id, creado_en);
CREATE INDEX idx_pago_estado ON pago (estado, creado_en);
