package com.tamaka.pagos.repository;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tamaka.pagos.service.PasarelaPagos;

/**
 * Adaptador de desarrollo de la pasarela. No cobra nada; genera una URL de checkout de sandbox.
 * Pendiente: adaptador real de la pasarela (PCI DSS: la plataforma nunca recibe datos de tarjeta).
 */
@Component
public class PasarelaPagosSandbox implements PasarelaPagos {

    @Override
    public String crearCheckout(String referenciaPasarela, BigDecimal monto) {
        return "https://sandbox.pasarela.tamaka.local/checkout/" + referenciaPasarela;
    }

    @Override
    public void reembolsar(String referenciaPasarela, BigDecimal monto) {
        // Sandbox: el reembolso se considera registrado.
    }
}
