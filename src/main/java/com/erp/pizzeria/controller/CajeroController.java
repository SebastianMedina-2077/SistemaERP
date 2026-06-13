package com.erp.pizzeria.controller;

import com.erp.pizzeria.service.CatalogService;
import com.erp.pizzeria.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CajeroController {

    private final CatalogService catalogService;
    private final PedidoService pedidoService;

    public CajeroController(CatalogService catalogService, PedidoService pedidoService) {
        this.catalogService = catalogService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/cajero/pos")
    public String pos(Model model) {
        model.addAttribute("categorias", catalogService.listCategorias());
        model.addAttribute("metodosPago", pedidoService.listMetodosPago());
        return "ventas/cajero";
    }
}
