package com.tamaka.cuentas.service;

import java.util.List;

import com.tamaka.cuentas.dto.request.CambiarModoRequest;
import com.tamaka.cuentas.dto.request.RegistrarCuentaRequest;
import com.tamaka.cuentas.dto.request.SuspensionRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.cuentas.dto.response.SesionResponse;

/**
 * Casos de uso de Cuentas.
 */
public interface CuentaService {

    CuentaResponse registrar(RegistrarCuentaRequest request);

    CuentaResponse buscarPorId(Long cuentaId);

    /** Listado de administracion. */
    List<CuentaResponse> listar();

    /** Habilita el rol OPERADOR y devuelve un token nuevo con los roles actualizados. */
    SesionResponse activarModoOperador(Long cuentaId);

    /** Alterna entre modo turista y modo operador; devuelve un token nuevo con los roles del modo elegido. */
    SesionResponse cambiarModo(Long cuentaId, CambiarModoRequest request);

    CuentaResponse cambiarSuspension(Long cuentaId, SuspensionRequest request);

    CuentaResponse asignarRolGestor(Long cuentaId);
}
