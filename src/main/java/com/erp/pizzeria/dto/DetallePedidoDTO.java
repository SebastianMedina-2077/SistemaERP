package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoDTO {

    @NotNull
    private Integer idProducto;

    @NotNull
    @Min(1)
    private Integer cantidad;

    @Size(max = 100)
    private String observacion;

    // Opcional: adicionales elegidos para esta linea (el precio lo resuelve el servidor).
    @Valid
    private List<AdicionalSeleccionadoDTO> adicionales;
}
