package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.DetallePedido;
import com.erp.pizzeria.model.Pedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCocinaDTO {

    private Integer idPedido;
    private String cliente;
    private LocalDateTime fecha;
    private String estado;
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private String producto;
        private Integer cantidad;
        private String observacion;

        public static Item from(DetallePedido d) {
            return Item.builder()
                    .producto(d.getProducto() != null ? d.getProducto().getNombre() : null)
                    .cantidad(d.getCantidad())
                    .observacion(d.getObservacion())
                    .build();
        }
    }

    public static PedidoCocinaDTO from(Pedido pedido, List<DetallePedido> detalles) {
        return PedidoCocinaDTO.builder()
                .idPedido(pedido.getIdPedido())
                .cliente(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null)
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : null)
                .items(detalles.stream().map(Item::from).toList())
                .build();
    }
}
