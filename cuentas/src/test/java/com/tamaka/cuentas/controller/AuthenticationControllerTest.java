package com.tamaka.cuentas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.tamaka.common.security.JwtService;
import com.tamaka.common.security.RestAccessDeniedHandler;
import com.tamaka.common.security.RestAuthenticationEntryPoint;
import com.tamaka.cuentas.dto.response.TokenResponse;
import com.tamaka.cuentas.exception.RefreshTokenInvalidoException;
import com.tamaka.cuentas.security.AuthenticationService;
import com.tamaka.cuentas.security.CuentaUserDetailsService;
import com.tamaka.cuentas.security.SecurityConfig;
import com.tamaka.cuentas.service.SesionService;

/**
 * Contrato HTTP de refresh y logout: publicos (sin access token), 401 con token invalido, 204 en logout.
 */
@WebMvcTest(controllers = AuthenticationController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private SesionService sesionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CuentaUserDetailsService userDetailsService;

    @Test
    void refreshValidoDevuelveParNuevoSinAccessToken() throws Exception {
        when(sesionService.renovar("rt-1")).thenReturn(new TokenResponse("jwt", "Bearer", 900, "rt-2", 604800));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt"))
                .andExpect(jsonPath("$.refreshToken").value("rt-2"));
    }

    @Test
    void refreshInvalidoResponde401() throws Exception {
        when(sesionService.renovar("robado")).thenThrow(new RefreshTokenInvalidoException());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"robado\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshSinCuerpoValidoResponde400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(sesionService, never()).renovar(any());
    }

    @Test
    void logoutResponde204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"rt-1\"}"))
                .andExpect(status().isNoContent());

        verify(sesionService).cerrarSesion("rt-1");
    }
}
