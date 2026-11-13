package com.tamaka.reservas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.dto.response.ReservaResponse;
import com.tamaka.reservas.security.SecurityConfig;
import com.tamaka.reservas.service.ReservaService;

/**
 * Contrato HTTP de GET /api/admin/reservas: solo ADMIN, filtro por estado validado (400 si no existe).
 */
@WebMvcTest(controllers = AdminReservaController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AdminReservaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void adminListaReservasFiltradasPorEstado() throws Exception {
        when(reservaService.listarTodas(EstadoReserva.CONFIRMADA)).thenReturn(List.of(
                new ReservaResponse(5L, 1L, 10L, 7L, LocalDate.of(2026, 12, 1), 2, "CONFIRMADA",
                        null, null, Instant.parse("2026-11-09T10:00:00Z"), null)));

        mockMvc.perform(get("/api/admin/reservas").param("estado", "CONFIRMADA"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void estadoInexistenteResponde400SinLlamarAlServicio() throws Exception {
        mockMvc.perform(get("/api/admin/reservas").param("estado", "PAGADA"))
                .andExpect(status().isBadRequest());

        verify(reservaService, never()).listarTodas(any());
    }

    @Test
    @WithMockUser(username = "1", roles = "TURISTA")
    void turistaRecibe403() throws Exception {
        mockMvc.perform(get("/api/admin/reservas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mockMvc.perform(get("/api/admin/reservas"))
                .andExpect(status().isUnauthorized());
    }
}
