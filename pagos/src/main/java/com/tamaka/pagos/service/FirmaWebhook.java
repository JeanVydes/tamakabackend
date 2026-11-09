package com.tamaka.pagos.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.tamaka.pagos.dto.request.WebhookPasarelaRequest;

/**
 * Firma HMAC-SHA256 de los webhooks. El mensaje canonico son los campos que definen el pago,
 * de modo que una firma valida no pueda reutilizarse para cambiar el monto o el estado.
 */
public final class FirmaWebhook {

    private static final String ALGORITMO = "HmacSHA256";

    private FirmaWebhook() {
    }

    public static String firmar(String secreto, WebhookPasarelaRequest request) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] digest = mac.doFinal(mensajeCanonico(request).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la firma del webhook", ex);
        }
    }

    /** Comparacion en tiempo constante. */
    public static boolean coincide(String firmaEsperada, String firmaRecibida) {
        if (firmaRecibida == null) {
            return false;
        }
        return MessageDigest.isEqual(
                firmaEsperada.getBytes(StandardCharsets.UTF_8),
                firmaRecibida.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    static String mensajeCanonico(WebhookPasarelaRequest request) {
        return request.referencia() + "|" + request.estado() + "|" + request.monto().toPlainString();
    }
}
