package com.tamaka.cuentas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tamaka.common.security.JwtService;
import com.tamaka.common.security.RestAccessDeniedHandler;
import com.tamaka.common.security.RestAuthenticationEntryPoint;
import com.tamaka.cuentas.dto.request.CambiarModoRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.cuentas.dto.response.SesionResponse;
import com.tamaka.cuentas.dto.response.TokenResponse;
import com.tamaka.cuentas.exception.ModoOperadorNoHabilitadoException;
import com.tamaka.cuentas.security.CuentaUserDetailsService;
import com.tamaka.cuentas.security.SecurityConfig;
import com.tamaka.cuentas.service.CuentaService;

/**
 * Contrato HTTP de PATCH /api/cuentas/me/modo: valida el cuerpo, devuelve el token nuevo y traduce la regla a 409.
 */
@WebMvcTest(controllers = CuentaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class CuentaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CuentaService cuentaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CuentaUserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "1", roles = "TURISTA")
    void cambiarModoDevuelveCuentaYTokenNuevo() throws Exception {
        CuentaResponse cuenta = new CuentaResponse(1L, "ana@tamaka.co", "Ana", true, true,
                Set.of("TURISTA", "OPERADOR"), Instant.parse("2026-11-09T10:00:00Z"));
        when(cuentaService.cambiarModo(eq(1L), any(CambiarModoRequest.class)))
                .thenReturn(new SesionResponse(cuenta, new TokenResponse("nuevo-token", "Bearer", 900, "nuevo-refresh", 604800)));

        mockMvc.perform(patch("/api/cuentas/me/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"OPERADOR\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.cuenta.modoOperador").value(true))
                .andExpect(jsonPath("$.token.accessToken").value("nuevo-token"));
    }

    @Test
    @WithMockUser(username = "1", roles = "TURISTA")
    void modoInvalidoResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(patch("/api/cuentas/me/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.modo").exists());

        verify(cuentaService, never()).cambiarModo(any(), any());
    }

    @Test
    @WithMockUser(username = "1", roles = "TURISTA")
    void sinRolOperadorHabilitadoResponde409() throws Exception {
        when(cuentaService.cambiarModo(eq(1L), any(CambiarModoRequest.class)))
                .thenThrow(new ModoOperadorNoHabilitadoException(1L));

        mockMvc.perform(patch("/api/cuentas/me/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"OPERADOR\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(patch("/api/cuentas/me/modo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modo\":\"OPERADOR\"}"))
                .andExpect(status().isUnauthorized());
    }
}
