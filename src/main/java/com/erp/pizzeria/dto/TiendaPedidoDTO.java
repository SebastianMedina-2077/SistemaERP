package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Cuerpo de un pedido de la tienda en linea: items + entrega + pagos. */
@Getter
@Setter
@NoArgsConstructor
public class TiendaPedidoDTO {

    @NotEmpty(message = "El pedido necesita al menos un producto")
    @Valid
    private List<DetallePedidoDTO> items;

    @NotNull(message = "Falta la informacion de entrega")
    @Valid
    private TiendaEntregaDTO entrega;

    @NotEmpty(message = "El pedido necesita al menos un pago")
    @Valid
    private List<TiendaPagoDTO> pagos;

    /** Codigo de cupon (opcional): el pedido re-cotiza con el, igual que el checkout. */
    @Size(max = 20)
    private String codigo;
}
