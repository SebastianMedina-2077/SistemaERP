package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemGuardadoDTO {
    private Integer idProducto;
    private String nombre;
    private BigDecimal precio;
    private Integer cantidad;
    private String observacion;
}
