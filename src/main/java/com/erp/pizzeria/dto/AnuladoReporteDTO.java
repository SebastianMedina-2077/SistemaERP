package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Fila plana de un pedido anulado para reportes y exportacion. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnuladoReporteDTO {
    private int numero;
    private String fecha;
    private String cliente;
    private String cajero;
    private BigDecimal total;
    private String motivo;
}
