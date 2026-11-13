-- Categoria del evento: la usa Recomendaciones para cruzarla con las preferencias del turista.
-- Las filas existentes quedan en GENERAL.
ALTER TABLE evento ADD COLUMN categoria VARCHAR(60) NOT NULL DEFAULT 'GENERAL';

CREATE INDEX idx_evento_categoria ON evento (categoria) WHERE activo;
