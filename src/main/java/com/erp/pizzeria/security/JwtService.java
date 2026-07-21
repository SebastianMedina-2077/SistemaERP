package com.erp.pizzeria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * Emision y validacion de los JWT (HS256) de la sesion de clientes de la tienda web.
 * Independiente de la sesion con cookie del personal interno.
 */
@Service
public class JwtService {

    private static final Duration VIGENCIA = Duration.ofHours(24);

    private final SecretKey clave;

    public JwtService(@Value("${tienda.jwt.secret}") String secreto) {
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    /** Emite un token para la cuenta: subject = id de cuenta, mas email y nombre. */
    public String emitir(Integer idCuenta, String email, String nombre) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(idCuenta))
                .claim("email", email)
                .claim("nombre", nombre)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + VIGENCIA.toMillis()))
                .signWith(clave)
                .compact();
    }

    /** Valida firma y expiracion; devuelve los claims o null si el token no sirve. */
    public Claims validar(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
