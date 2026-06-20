package com.erp.pizzeria.controller;

import com.erp.pizzeria.repository.PedidoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/caja")
public class CajaController {

    private final PedidoRepository pedidoRepository;

    public CajaController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping("/resumen-dia")
    public Map<String, Object> resumenDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.atStartOfDay();
        LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("fecha", hoy.toString());
        resumen.put("totalPedidos", pedidoRepository.contarPorRango(desde, hasta));
        BigDecimal ventas = pedidoRepository.sumarVentasPorRango(desde, hasta);
        resumen.put("totalVentas", ventas != null ? ventas : BigDecimal.ZERO);
        return resumen;
    }

    // Stub: el cierre de dia aun no consolida nada. Pendiente de definir reglas de negocio.
    @PostMapping("/cerrar-dia")
    public ResponseEntity<Map<String, Object>> cerrarDia() {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("ok", false);
        respuesta.put("mensaje", "El cierre de dia aun no esta implementado. Faltan definir las reglas de negocio.");
        return ResponseEntity.ok(respuesta);
    }
}
