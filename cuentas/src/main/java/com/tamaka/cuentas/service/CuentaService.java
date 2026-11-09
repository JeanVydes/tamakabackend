package com.tamaka.cuentas.service;

import com.tamaka.cuentas.dto.request.RegistrarCuentaRequest;
import com.tamaka.cuentas.dto.request.SuspensionRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;

/**
 * Casos de uso de Cuentas.
 */
public interface CuentaService {

    CuentaResponse registrar(RegistrarCuentaRequest request);

    CuentaResponse buscarPorId(Long cuentaId);

    CuentaResponse activarModoOperador(Long cuentaId);

    CuentaResponse cambiarSuspension(Long cuentaId, SuspensionRequest request);

    CuentaResponse asignarRolGestor(Long cuentaId);
}
