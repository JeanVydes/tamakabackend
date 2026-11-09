package com.tamaka.common.security;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;

/**
 * Valida (todos los servicios) y emite (solo Cuentas) tokens JWT RS256.
 * La firma se verifica antes de leer cualquier claim.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties properties;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.publicKey = parsePublicKey(properties.publicKey());
        this.privateKey = properties.privateKey() == null || properties.privateKey().isBlank()
                ? null
                : parsePrivateKey(properties.privateKey());
    }

    /**
     * Valida el token y construye la identidad. Devuelve vacio si la firma, el issuer o la expiracion no son validos.
     * No registra el token en logs.
     */
    public Optional<Authentication> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            List<SimpleGrantedAuthority> authorities = rolesOf(claims).stream()
                    .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol))
                    .toList();

            return Optional.of(new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities));
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token rechazado: {}", ex.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /**
     * Emite un access token firmado con la clave privada. Solo lo usa Cuentas.
     */
    public String issue(String subject, Collection<String> roles) {
        if (privateKey == null) {
            throw new IllegalStateException("Este servicio no tiene clave privada para emitir tokens");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(subject)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(properties.accessTokenTtl())))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @SuppressWarnings("unchecked")
    private Set<String> rolesOf(Claims claims) {
        Object raw = claims.get(CLAIM_ROLES);
        if (raw instanceof Collection<?> coleccion) {
            return coleccion.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }

    private static PublicKey parsePublicKey(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Falta tamaka.security.jwt.public-key");
        }
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Clave publica JWT invalida", ex);
        }
    }

    private static PrivateKey parsePrivateKey(String base64) {
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception ex) {
            throw new IllegalStateException("Clave privada JWT invalida", ex);
        }
    }
}
