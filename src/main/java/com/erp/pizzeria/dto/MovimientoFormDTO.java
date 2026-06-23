package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class MovimientoFormDTO {

    @NotNull(message = "Seleccione un tipo de movimiento")
    private Integer idTipoMovimiento;

    @Size(max = 15, message = "Maximo 15 caracteres")
    private String documento;

    @Size(max = 25, message = "Maximo 25 caracteres")
    private String glosa;

    @Valid
    @NotEmpty(message = "Agregue al menos un insumo")
    private List<MovimientoLineaDTO> lineas = new ArrayList<>();
}
