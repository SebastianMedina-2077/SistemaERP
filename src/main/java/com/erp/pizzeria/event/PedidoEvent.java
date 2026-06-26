package com.erp.pizzeria.event;

import java.util.Map;

/**
 * Evento de dominio para empujar cambios de pedido a las pantallas por SSE.
 * tipo: "pedido-nuevo" (lo crea el cajero) o "pedido-estado" (lo mueve la cocina).
 */
public record PedidoEvent(String tipo, Map<String, Object> payload) {
}
