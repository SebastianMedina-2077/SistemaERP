package com.erp.pizzeria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Una parte de pago: metodo + monto. Varias forman un pago mixto. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {

    @NotNull
    private Integer idMetodoPago;

    @NotNull
    @Positive
    private BigDecimal monto;
}
