-- Imagen de portada del evento (el bucket guarda el objeto; aqui solo la clave).
ALTER TABLE evento ADD COLUMN imagen_key VARCHAR(512);
