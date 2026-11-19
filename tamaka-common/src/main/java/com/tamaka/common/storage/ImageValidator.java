package com.tamaka.common.storage;

import java.util.Set;
import java.util.UUID;

/**
 * Reglas de las imagenes de ofertas (atractivos, comercios/servicios, eventos) y del avatar:
 * un unico tipo permitido por entidad, JPEG/PNG/WebP, 5 MB como maximo. Sin librerias de imagen:
 * la validacion es por tipo MIME declarado + firma del archivo (magic bytes).
 */
public final class ImageValidator {

    public static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    public static final String EXTENSION = ".jpg";

    private ImageValidator() {
    }

    /**
     * Valida tipo declarado y firma del archivo.
     *
     * @param contentType tipo MIME que manda el cliente (p. ej. image/jpeg)
     * @param cabeza      primeros bytes del archivo (al menos 12)
     * @return la extension segura a usar al guardar en el bucket
     * @throws ImagenInvalidaException si el tipo no esta permitido o el contenido no coincide
     */
    public static String validar(String contentType, byte[] cabeza) {
        TiposImagen.verificarFirma(contentType, cabeza);
        return TiposImagen.extensionDe(contentType);
    }

    /** Clave nueva y unica dentro de un prefijo (p. ej. atractivos/42/<uuid>.jpg). */
    public static String nuevaClave(String prefijo, String id, String extension) {
        return prefijo + "/" + id + "/" + UUID.randomUUID() + extension;
    }
}
