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
public class CajaReporteDTO {
    private BigDecimal montoInicial;
    private BigDecimal efectivo;
    private BigDecimal tarjeta;
    private BigDecimal yape;
    private BigDecimal plin;
    private BigDecimal totalVentas;
    private BigDecimal efectivoEsperado;
}
