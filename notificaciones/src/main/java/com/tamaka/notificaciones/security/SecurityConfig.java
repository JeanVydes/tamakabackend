package com.tamaka.notificaciones.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.tamaka.common.security.JwtAuthenticationFilter;
import com.tamaka.common.security.JwtService;
import com.tamaka.common.security.RestAccessDeniedHandler;
import com.tamaka.common.security.RestAuthenticationEntryPoint;

/**
 * Capa de seguridad de Notificaciones: cualquier usuario autenticado, solo sobre sus propias notificaciones.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtService jwtService,
                                            RestAuthenticationEntryPoint entryPoint,
                                            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Bus de eventos interno: se autentica con el secreto compartido, no con JWT.
                        .requestMatchers(HttpMethod.POST, "/internal/eventos").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
