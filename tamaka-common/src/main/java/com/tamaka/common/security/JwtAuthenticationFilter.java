package com.tamaka.common.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de la capa de seguridad: lee "Authorization: Bearer", valida el token y deja la identidad
 * en el SecurityContextHolder. Sin token valido la peticion continua anonima y se rechaza en la regla de acceso (401).
 * No es un @Component: se registra solo en la SecurityFilterChain de cada servicio.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            String token = header.substring(BEARER.length()).trim();
            jwtService.parse(token)
                    .ifPresent(autenticacion -> SecurityContextHolder.getContext().setAuthentication(autenticacion));
        }
        chain.doFilter(request, response);
    }
}
