-- Imagen de portada del atractivo (la sirve Cloudflare R2; aqui solo guardamos la clave).
ALTER TABLE atractivo ADD COLUMN imagen_key VARCHAR(512);
