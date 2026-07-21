package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Respuesta al crear un pedido desde la tienda en linea. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiendaPedidoCreadoDTO {

    private Integer idPedido;
    private BigDecimal total;
    private String estado;
}
