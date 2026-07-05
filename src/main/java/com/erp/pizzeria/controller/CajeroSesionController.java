package com.erp.pizzeria.controller;

import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.service.CajaStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Salida del cajero. Un cajero con la caja abierta no puede cerrar sesion:
 * debe cuadrar y cerrar el turno primero. Si no tiene caja abierta, hace logout normal.
 */
@Controller
public class CajeroSesionController {

    private final CajaStore cajaStore;
    private final UsuarioRepository usuarioRepository;

    public CajeroSesionController(CajaStore cajaStore, UsuarioRepository usuarioRepository) {
        this.cajaStore = cajaStore;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/cajero/salir")
    public String salir(Authentication auth, HttpServletRequest request) {
        Usuario u = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (u != null && cajaStore.obtener(u.getIdUsuario()) != null) {
            return "redirect:/cajero/pos";
        }
        new SecurityContextLogoutHandler().logout(request, null, auth);
        SecurityContextHolder.clearContext();
        return "redirect:/login?logout";
    }
}
