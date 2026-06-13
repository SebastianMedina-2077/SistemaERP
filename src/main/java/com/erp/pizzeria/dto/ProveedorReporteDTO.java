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
public class ProveedorReporteDTO {
    private String proveedor;
    private long compras;
    private BigDecimal total;
    private String ultimaCompra;
}
