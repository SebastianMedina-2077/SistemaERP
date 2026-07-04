package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.BoletaImpresionDTO;
import com.erp.pizzeria.service.BoletaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Vista imprimible de la boleta (ticket) para el modulo del cajero.
 * Protegida bajo /cajero/** (rol CAJERO en SecurityConfig).
 */
@Controller
public class BoletaController {

    private final BoletaService boletaService;

    public BoletaController(BoletaService boletaService) {
        this.boletaService = boletaService;
    }

    @GetMapping("/cajero/boleta/{idPedido}")
    public String boleta(@PathVariable Integer idPedido, Model model) {
        BoletaImpresionDTO boleta = boletaService.construirImpresion(idPedido);
        model.addAttribute("boleta", boleta);
        return "ventas/boleta";
    }
}