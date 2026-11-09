package com.tamaka.pagos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.pagos.dto.request.IniciarPagoRequest;
import com.tamaka.pagos.dto.request.WebhookPasarelaRequest;
import com.tamaka.pagos.dto.response.ComprobanteResponse;
import com.tamaka.pagos.dto.response.PagoResponse;
import com.tamaka.pagos.service.PagoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private static final String CABECERA_FIRMA = "X-Firma-Pasarela";

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<PagoResponse> iniciar(Authentication authentication,
                                                @Valid @RequestBody IniciarPagoRequest request) {
        PagoResponse pago = pagoService.iniciar(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @GetMapping("/mis")
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<List<PagoResponse>> misPagos(Authentication authentication) {
        return ResponseEntity.ok(pagoService.misPagos(Long.valueOf(authentication.getName())));
    }

    @GetMapping("/{id}/comprobante")
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<ComprobanteResponse> comprobante(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(pagoService.comprobante(Long.valueOf(authentication.getName()), id));
    }

    /** Sin JWT: se autentica con la firma HMAC de la pasarela (verificada en el servicio). */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestHeader(value = CABECERA_FIRMA, required = false) String firma,
                                        @Valid @RequestBody WebhookPasarelaRequest request) {
        pagoService.procesarWebhook(firma, request);
        return ResponseEntity.ok().build();
    }
}
