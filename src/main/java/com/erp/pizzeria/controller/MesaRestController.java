package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.MesaDTO;
import com.erp.pizzeria.service.MesaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de mesas del salon para la caja (rol CAJERO, ver SecurityConfig).
 * Solo expone la lectura y las dos transiciones que dispara el cajero;
 * OCUPADA la pone el propio flujo de venta al crear el pedido.
 */
@RestController
public class MesaRestController {

    private final MesaService mesaService;

    public MesaRestController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @GetMapping("/api/mesas")
    public List<MesaDTO> listar() {
        return mesaService.listar();
    }

    /** OCUPADA -> POR_LIMPIAR: el cliente se retiro y la mesa espera limpieza. */
    @PatchMapping("/api/mesas/{numero}/limpiar")
    public MesaDTO mandarALimpiar(@PathVariable int numero) {
        return mesaService.mandarALimpiar(numero);
    }

    /** POR_LIMPIAR -> LIBRE: la mesa quedo limpia y vuelve a estar disponible. */
    @PatchMapping("/api/mesas/{numero}/liberar")
    public MesaDTO marcarLimpia(@PathVariable int numero) {
        return mesaService.marcarLimpia(numero);
    }
}
