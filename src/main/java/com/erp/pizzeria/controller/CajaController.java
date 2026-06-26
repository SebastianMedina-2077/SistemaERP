package com.erp.pizzeria.controller;

import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.PagoRepository;
import com.erp.pizzeria.repository.PedidoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/caja")
public class CajaController {

    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;

    public CajaController(PedidoRepository pedidoRepository, PagoRepository pagoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pagoRepository = pagoRepository;
    }

    @GetMapping("/resumen-dia")
    public Map<String, Object> resumenDia() {
        return consolidadoDelDia();
    }

    /**
     * Cierre de dia: consolida ventas y desglose por metodo de pago de la fecha de hoy
     * (el Z-report del turno operativo).
     *
     * Genera el consolidado pero no persiste ni "bloquea" el dia: no hay tabla de cierre.
     * Si luego se necesita historial o impedir reapertura, agregar una tabla cierre_dia y
     * marcar aqui la fecha como cerrada.
     */
    @PostMapping("/cerrar-dia")
    public Map<String, Object> cerrarDia() {
        Map<String, Object> consolidado = consolidadoDelDia();
        consolidado.put("ok", true);
        consolidado.put("mensaje", "Dia cerrado. Resumen consolidado generado.");
        return consolidado;
    }

    private Map<String, Object> consolidadoDelDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.atStartOfDay();
        LocalDateTime hasta = hoy.plusDays(1).atStartOfDay();

        BigDecimal ventas = pedidoRepository.sumarVentasPorRango(desde, hasta);
        ventas = ventas != null ? ventas : BigDecimal.ZERO;

        List<Map<String, Object>> porMetodo = new ArrayList<>();
        for (Object[] fila : pagoRepository.resumenPorMetodoRango(desde, hasta, EstadoPedido.ANULADO)) {
            Map<String, Object> linea = new LinkedHashMap<>();
            linea.put("metodo", fila[0]);
            linea.put("monto", fila[1]);
            porMetodo.add(linea);
        }

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("fecha", hoy.toString());
        resumen.put("totalPedidos", pedidoRepository.contarPorRango(desde, hasta));
        resumen.put("totalVentas", ventas);
        resumen.put("ventasPorMetodo", porMetodo);
        return resumen;
    }
}
