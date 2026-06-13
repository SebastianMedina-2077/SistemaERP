package com.erp.pizzeria.controller;

import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserAdvice {

    public record CurrentUserView(String nombre, String rol) {}

    private final UsuarioRepository usuarioRepository;

    public CurrentUserAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("currentUser")
    public CurrentUserView currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return usuarioRepository.findByUsername(authentication.getName())
                .map(this::toView)
                .orElse(new CurrentUserView(authentication.getName(), ""));
    }

    private CurrentUserView toView(Usuario u) {
        String nombre = u.getEmpleado() != null
                ? u.getEmpleado().getNombre() + " " + u.getEmpleado().getApellido()
                : u.getUsername();
        String rol = u.getRol() != null ? u.getRol().getNombre() : "";
        return new CurrentUserView(nombre, rol);
    }
}
