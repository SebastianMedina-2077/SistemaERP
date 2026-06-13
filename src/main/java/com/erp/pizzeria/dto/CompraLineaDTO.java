package com.erp.pizzeria.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraLineaDTO {

    @NotNull
    private Integer idInsumo;

    @NotNull
    @DecimalMin("0.001")
    private BigDecimal cantidad;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal precioUnitario;
}
