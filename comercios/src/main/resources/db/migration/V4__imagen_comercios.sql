-- Imagen de portada (el bucket guarda el objeto; aqui solo la clave).
ALTER TABLE comercio ADD COLUMN imagen_key VARCHAR(512);
ALTER TABLE servicio ADD COLUMN imagen_key VARCHAR(512);
