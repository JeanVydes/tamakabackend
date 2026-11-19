package com.tamaka.common.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Puente con el bucket de imagenes (Cloudflare R2, compatible con S3).
 * Si el entorno no configura las claves (TAMAKA_STORAGE_*), disponible() es falso
 * y todo uso lanza StorageNotAvailableException — la API lo traduce a 503.
 */
public class ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ImageStorage.class);

    private final StorageProperties props;
    private final S3Client s3;

    public ImageStorage(StorageProperties props) {
        this.props = props;
        this.s3 = props.disponible()
                ? S3Client.builder()
                        .endpointOverride(URI.create(props.endpoint()))
                        .region(Region.US_EAST_1) // R2 lo ignora, pero el SDK lo exige
                        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                        .credentialsProvider(() -> AwsBasicCredentials.create(props.accessKeyId(), props.secretAccessKey()))
                        .build()
                : null;
    }

    public boolean disponible() {
        return s3 != null;
    }

    /**
     * Valida y sube el archivo.
     *
     * @return la clave del objeto guardado (ej. atractivos/3/<uuid>.jpg)
     */
    public String subir(String carpeta, String id, MultipartFile archivo) {
        exigirDisponible();
        byte[] cuerpo;
        try {
            // El limite real lo impone spring.servlet.multipart antes de llegar aqui;
            // se revalida por defensa en profundidad.
            cuerpo = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen recibida", e);
        }
        if (cuerpo.length == 0) {
            throw new ImagenInvalidaException("imagen", "La imagen esta vacia");
        }
        if (cuerpo.length > TiposImagen.MAX_BYTES) {
            throw new ImagenInvalidaException("imagen", "La imagen supera el maximo de 5 MB");
        }
        String mime = normalizarMime(archivo.getContentType());
        if (mime == null || !TiposImagen.MIME_A_FORMATO.containsKey(mime)) {
            throw new ImagenInvalidaException("imagen",
                    "Formato de imagen no admitido: usa JPEG, PNG o WebP");
        }
        byte[] cabeza = java.util.Arrays.copyOf(cuerpo, Math.min(cuerpo.length, 12));
        String extension = ImageValidator.validar(mime, cabeza);
        String clave = ImageValidator.nuevaClave(carpeta, id, extension);
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(props.bucket())
                            .key(clave)
                            .contentType(mime)
                            .contentLength((long) cuerpo.length)
                            .build(),
                    RequestBody.fromBytes(cuerpo));
        } catch (SdkClientException e) {
            log.error("No se pudo subir la imagen {} a R2", clave, e);
            throw new StorageNotAvailableException();
        }
        return clave;
    }

    /** Borra el objeto si existe; nunca lanza (la imagen es un extra de la entidad). */
    public void borrar(String clave) {
        if (clave == null || !disponible()) {
            return;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket()).key(clave).build());
        } catch (RuntimeException e) {
            log.warn("No se pudo borrar {} en R2: {}", clave, e.getMessage());
        }
    }

    /** URL publica absoluta (el bucket se sirve por su dominio publico, ej. media.taminaka.com). */
    public String urlPublica(String clave) {
        return clave == null ? null : props.publicBaseUrl() + "/" + clave;
    }

    public void exigirDisponible() {
        if (!disponible()) {
            throw new StorageNotAvailableException();
        }
    }

    private static String normalizarMime(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
