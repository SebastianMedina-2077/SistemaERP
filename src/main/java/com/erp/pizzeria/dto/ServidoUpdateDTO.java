package com.erp.pizzeria.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuerpo del PATCH que marca/desmarca un item como servido en el KDS. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServidoUpdateDTO {

    @NotNull
    private Boolean servido;
}
