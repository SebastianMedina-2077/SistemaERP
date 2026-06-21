package com.erp.pizzeria.controller;

import com.erp.pizzeria.service.AuditoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping("/auditoria")
    public String auditoria(Model model) {
        model.addAttribute("active", "auditoria");
        model.addAttribute("pageTitle", "Auditoria");
        model.addAttribute("registros", auditoriaService.listar());
        return "admin/auditoria";
    }
}
