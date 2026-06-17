package com.erp.pizzeria.controller;

import com.erp.pizzeria.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints de la cuenta del usuario autenticado.
 * Se usa para confirmar identidad antes de operaciones sensibles:
 *  - desbloqueo de pantalla (bloqueo por inactividad / toggle de sesion)
 *  - creacion de un nuevo usuario con rol Administrador
 */
@RestController
@RequestMapping("/api/account")
public class AccountRestController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountRestController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record VerifyPasswordRequest(String password) {
    }

    /**
     * Verifica que la contrasena enviada coincide con la del usuario actualmente autenticado.
     * Nunca recibe el username desde el cliente: usa el de la sesion de Spring Security.
     */
    @PostMapping("/verify-password")
    public Map<String, Boolean> verifyPassword(@RequestBody VerifyPasswordRequest request,
                                               Authentication authentication) {
        boolean valido = authentication != null
                && authentication.isAuthenticated()
                && request.password() != null
                && usuarioRepository.findByUsername(authentication.getName())
                        .map(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                        .orElse(false);
        return Map.of("valido", valido);
    }
}