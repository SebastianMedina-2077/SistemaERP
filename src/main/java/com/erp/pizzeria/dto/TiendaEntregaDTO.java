package com.erp.pizzeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos de entrega de un pedido web: DELIVERY (con direccion) o RECOJO. */
@Getter
@Setter
@NoArgsConstructor
public class TiendaEntregaDTO {

    @NotBlank(message = "Elige la modalidad de entrega")
    private String modalidad;

    @Size(max = 150, message = "La direccion admite hasta 150 caracteres")
    private String direccion;
}
