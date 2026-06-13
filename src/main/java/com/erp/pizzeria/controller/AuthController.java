package com.erp.pizzeria.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (roles.contains("ROLE_ADMINISTRADOR")) {
            return "redirect:/admin/dashboard";
        }
        if (roles.contains("ROLE_CAJERO")) {
            return "redirect:/cajero/pos";
        }
        if (roles.contains("ROLE_COCINA")) {
            return "redirect:/cocina";
        }
        return "redirect:/login";
    }
}
