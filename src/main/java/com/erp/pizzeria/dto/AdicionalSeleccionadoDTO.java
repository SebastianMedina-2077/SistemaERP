package com.erp.pizzeria.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Adicional elegido por el cliente en una linea: solo id y cantidad; el precio lo pone el servidor. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdicionalSeleccionadoDTO {

    @NotNull
    private Integer idAdicional;

    @NotNull
    @Min(1)
    private Integer cantidad;
}
