package com.tamaka.pagos.service;

import java.math.BigDecimal;

/**
 * Puerto de salida hacia la pasarela de pagos externa. La plataforma no guarda datos de tarjeta.
 */
public interface PasarelaPagos {

    /** Crea la sesion de cobro y devuelve la URL donde el turista paga fuera de la plataforma. */
    String crearCheckout(String referenciaPasarela, BigDecimal monto);

    void reembolsar(String referenciaPasarela, BigDecimal monto);
}
