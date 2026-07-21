package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Una parte del pago de un pedido web: metodo por nombre + monto (varias = pago mixto). */
@Getter
@Setter
@NoArgsConstructor
public class TiendaPagoDTO {

    /** EFECTIVO, TARJETA, YAPE o PLIN (se resuelve contra metodo_pago en BD). */
    @NotBlank(message = "Indica el metodo de pago")
    private String metodo;

    @NotNull(message = "Indica el monto del pago")
    @Positive(message = "Cada pago debe ser mayor a cero")
    private BigDecimal monto;

    /** Solo para metodo TARJETA. */
    @Valid
    private TiendaTarjetaDTO tarjeta;
}
