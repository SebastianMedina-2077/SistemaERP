package com.erp.pizzeria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen de un pedido web para "Mis pedidos": estado actual de la venta,
 * modalidad de entrega y las lineas resumidas (nombre + cantidad).
 */
public record TiendaPedidoResumenDTO(
        Integer idPedido,
        LocalDateTime fecha,
        BigDecimal total,
        String estado,
        String modalidad,
        List<Item> items) {

    public record Item(String nombre, Integer cantidad) {
    }
}
