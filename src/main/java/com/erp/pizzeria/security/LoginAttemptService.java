package com.erp.pizzeria.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno de fuerza bruta: tras MAX_INTENTOS fallidos seguidos, bloquea al usuario por BLOQUEO.
 *
 * Conteo en memoria y por nombre de usuario. Un atacante podria bloquear a una victima a
 * proposito (DoS de cuenta); aceptable para un ERP interno. Para algo mas fuerte, llevar el
 * conteo por IP y a un store compartido.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Intento(int fallos, Instant bloqueadoHasta) {
    }

    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String usuario) {
        Intento i = intentos.get(clave(usuario));
        return i != null && i.bloqueadoHasta() != null && Instant.now().isBefore(i.bloqueadoHasta());
    }

    public void registrarFallo(String usuario) {
        intentos.compute(clave(usuario), (k, previo) -> {
            int fallos = (previo == null ? 0 : previo.fallos()) + 1;
            Instant hasta = fallos >= MAX_INTENTOS ? Instant.now().plus(BLOQUEO) : null;
            return new Intento(fallos, hasta);
        });
    }

    public void registrarExito(String usuario) {
        intentos.remove(clave(usuario));
    }

    private String clave(String usuario) {
        return usuario == null ? "" : usuario.toLowerCase();
    }
}
