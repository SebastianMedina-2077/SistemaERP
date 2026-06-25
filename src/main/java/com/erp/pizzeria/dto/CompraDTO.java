package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompraDTO {

    @NotNull(message = "Seleccione un proveedor")
    private Integer idProveedor;

    @NotEmpty(message = "Agregue al menos un insumo a la compra")
    @Valid
    private List<CompraLineaDTO> items = new ArrayList<>();
}
