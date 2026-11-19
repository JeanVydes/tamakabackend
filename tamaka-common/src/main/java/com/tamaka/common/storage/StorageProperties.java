package com.tamaka.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del objeto bucket (Cloudflare R2, compatible con S3).
 * Si el endpoint queda vacio, el almacenamiento esta deshabilitado y la subida responde 503.
 *
 * @param endpoint       https://<account-id>.r2.cloudflarestorage.com (el bucket en el path, si lo trae, se quita)
 * @param bucket         nombre del bucket (p. ej. vyde-cdn)
 * @param publicBaseUrl  URL publica base del bucket (p. ej. https://cdn.vyde.dev), con o sin esquema y sin slash final
 * @param accessKeyId    llave del token de API de R2 (solo permisos de objeto)
 * @param secretAccessKey secreto del token
 */
@ConfigurationProperties(prefix = "tamaka.storage")
public record StorageProperties(
        String endpoint,
        String bucket,
        String publicBaseUrl,
        String accessKeyId,
        String secretAccessKey) {

    public StorageProperties {
        endpoint = vacia(endpoint);
        bucket = vacia(bucket);
        publicBaseUrl = vacia(publicBaseUrl);
        accessKeyId = vacia(accessKeyId);
        secretAccessKey = vacia(secretAccessKey);
        // El .env del servidor suele traer el endpoint con el bucket en el path: .../vyde-cdn
        if (!endpoint.isBlank() && !bucket.isBlank()) {
            String path = java.net.URI.create(endpoint).getPath();
            String suffix = "/" + bucket;
            if (path != null && path.endsWith(suffix)) {
                endpoint = endpoint.substring(0, endpoint.length() - suffix.length());
            }
            endpoint = sinSlashFinal(endpoint);
        }
        if (!publicBaseUrl.isBlank()) {
            publicBaseUrl = sinComillas(publicBaseUrl);
            if (!publicBaseUrl.startsWith("http://") && !publicBaseUrl.startsWith("https://")) {
                publicBaseUrl = "https://" + publicBaseUrl;
            }
            publicBaseUrl = sinSlashFinal(sinComillas(publicBaseUrl));
        }
    }

    private static String vacia(String v) {
        return v == null ? "" : sinComillas(v).trim();
    }

    private static String sinComillas(String v) {
        v = v.trim();
        while ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private static String sinSlashFinal(String v) {
        String out = v;
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    public boolean disponible() {
        return !endpoint.isBlank() && !bucket.isBlank() && !publicBaseUrl.isBlank()
                && !accessKeyId.isBlank() && !secretAccessKey.isBlank();
    }
}
