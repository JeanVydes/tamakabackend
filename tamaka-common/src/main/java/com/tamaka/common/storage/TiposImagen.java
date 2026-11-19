package com.tamaka.common.storage;

import java.util.Locale;
import java.util.Map;

/**
 * Mapea MIME declarado -> extension y magic bytes de cada formato soportado.
 * El tamano lo imponen los limites multipart (5 MB); aqui se defiende en profundidad.
 */
final class TiposImagen {

    static final long MAX_BYTES = 5L * 1024 * 1024;

    record Formato(String extension, String mime) {
    }

    static final Map<String, Formato> MIME_A_FORMATO = Map.of(
            "image/jpeg", new Formato(".jpg", "image/jpeg"),
            "image/png", new Formato(".png", "image/png"),
            "image/webp", new Formato(".webp", "image/webp"));

    private TiposImagen() {
    }

    /** Extension segura: sin ella no se puede confiar en el nombre que mande el cliente. */
    static String extensionDe(String contentType) {
        if (contentType == null) {
            throw new ImagenInvalidaException("imagen", "No se recibio un tipo de archivo valido");
        }
        Formato formato = MIME_A_FORMATO.get(contentType.split(";")[0].trim().toLowerCase(Locale.ROOT));
        if (formato == null) {
            throw new ImagenInvalidaException("imagen",
                    "Formato de imagen no admitido: usa JPEG, PNG o WebP");
        }
        return formato.extension();
    }

    /** El tipo declarado debe coincidir con la firma real del archivo. */
    static void verificarFirma(String contentType, byte[] head) {
        String mime = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        Formato formato = MIME_A_FORMATO.get(mime);
        if (formato == null || head == null || head.length < 12) {
            throw new ImagenInvalidaException("imagen", "El archivo no es una imagen valida");
        }
        boolean firmaOk = switch (formato.extension()) {
            case ".jpg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case ".png" -> (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47;
            case ".webp" -> head[0] == 0x52 && head[1] == 0x49 && head[2] == 0x46 && head[3] == 0x46  // "RIFF"
                    && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50;      // "WEBP"
            default -> false;
        };
        if (!firmaOk) {
            throw new ImagenInvalidaException("imagen",
                    "El contenido del archivo no corresponde al formato declarado");
        }
    }
}
