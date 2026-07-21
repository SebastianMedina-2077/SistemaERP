package com.erp.pizzeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuerpo de la validacion de codigo promocional del checkout de la tienda. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiendaPromoValidacionRequestDTO {

    @NotBlank
    private String codigo;
}
