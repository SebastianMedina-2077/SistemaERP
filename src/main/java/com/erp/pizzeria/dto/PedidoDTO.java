package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class PedidoDTO {

    @NotNull
    @Size(max = 15)
    private String clienteNombre;

    @Size(max = 9)
    private String clienteTelefono;

    @NotNull
    private Integer idMetodoPago;

    @NotEmpty
    @Valid
    private List<DetallePedidoDTO> items;
}
