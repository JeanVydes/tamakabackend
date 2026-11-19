-- Foto de perfil de la cuenta (el bucket guarda el objeto; aqui solo la clave).
ALTER TABLE cuenta ADD COLUMN imagen_key VARCHAR(512);
