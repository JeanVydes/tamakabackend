-- Outbox transaccional (tamaka-common, JdbcOutboxStore): el evento se inserta en la misma transaccion que el
-- cambio de estado y el relay lo entrega despues. Estados: PENDIENTE, ENVIADO, FALLIDO (revision manual).
-- La numeracion V3 es comun a todos los servicios; Flyway admite huecos en las versiones.
CREATE TABLE outbox_evento (
    id               VARCHAR(36)   PRIMARY KEY,   -- id unico del evento (UUID), el mismo que reciben los consumidores
    tipo             VARCHAR(80)   NOT NULL,
    agregado_id      VARCHAR(64)   NOT NULL,
    payload          TEXT          NOT NULL,      -- DomainEvent en JSON
    estado           VARCHAR(20)   NOT NULL,
    intentos         INTEGER       NOT NULL DEFAULT 0,
    proximo_intento  TIMESTAMPTZ   NOT NULL,
    creado_en        TIMESTAMPTZ   NOT NULL,
    enviado_en       TIMESTAMPTZ,
    ultimo_error     VARCHAR(500),

    CONSTRAINT ck_outbox_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    CONSTRAINT ck_outbox_intentos CHECK (intentos >= 0)
);

-- El relay busca PENDIENTE con proximo_intento vencido, en orden de creacion.
CREATE INDEX idx_outbox_pendientes ON outbox_evento (proximo_intento, creado_en) WHERE estado = 'PENDIENTE';
