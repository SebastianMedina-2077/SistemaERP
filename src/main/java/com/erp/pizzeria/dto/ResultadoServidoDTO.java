package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resultado autoritativo de marcar un item como servido: el valor aplicado,
 * si con eso quedaron todos servidos y el estado del pedido tras el cambio
 * (puede ser ATENDIDO si el servidor lo cerro automaticamente).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoServidoDTO {

    private Integer idPedido;
    private boolean servidoActualizado;
    private boolean todosServidos;
    private String nuevoEstado;
}
