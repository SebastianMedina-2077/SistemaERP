package com.erp.pizzeria.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verificacion de correo por codigo para el registro de la tienda web. El codigo de
 * 6 digitos vive SOLO en memoria (nunca se persiste en BD), por email, y vence a los
 * 10 minutos. Es de un solo uso: al validarlo correctamente se consume.
 *
 * Patron simple en memoria, como {@link com.erp.pizzeria.security.LoginAttemptService}.
 * Single-instance: si se escala a varias replicas habria que mover los codigos a un
 * store compartido (Redis).
 */
@Service
public class VerificacionCorreoService {

    private static final Duration VIGENCIA = Duration.ofMinutes(10);

    private record Codigo(String valor, Instant expira) {
    }

    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Codigo> codigos = new ConcurrentHashMap<>();
    private final EmailComprobanteService emailComprobanteService;

    public VerificacionCorreoService(EmailComprobanteService emailComprobanteService) {
        this.emailComprobanteService = emailComprobanteService;
    }

    /**
     * Genera un codigo nuevo de 6 digitos para el email, reemplaza cualquiera anterior
     * y lo envia por correo. El envio nunca rompe el flujo (si el SMTP no esta
     * configurado, solo se registra el aviso).
     */
    public void generarYEnviar(String email) {
        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        codigos.put(clave(email), new Codigo(codigo, Instant.now().plus(VIGENCIA)));
        emailComprobanteService.enviarCodigoVerificacion(email, codigo);
    }

    /**
     * Valida el codigo del email: debe ser el correcto y no estar vencido. Al acertar
     * se consume (un solo uso). Un codigo vencido se descarta.
     */
    public boolean validar(String email, String codigo) {
        if (codigo == null) {
            return false;
        }
        Codigo actual = codigos.get(clave(email));
        if (actual == null) {
            return false;
        }
        if (Instant.now().isAfter(actual.expira())) {
            codigos.remove(clave(email));
            return false;
        }
        if (!actual.valor().equals(codigo.trim())) {
            return false;
        }
        codigos.remove(clave(email));
        return true;
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
