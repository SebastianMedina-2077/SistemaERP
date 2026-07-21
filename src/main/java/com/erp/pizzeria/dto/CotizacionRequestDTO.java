package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Cuerpo del preview de precios: la lista de items a cotizar (reutiliza DetallePedidoDTO). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionRequestDTO {

    @Valid
    @NotEmpty
    private List<DetallePedidoDTO> items;

    /** Codigo de cupon (opcional): si es valido y esta activo, la cotizacion aplica su descuento. */
    @Size(max = 20)
    private String codigo;
}
