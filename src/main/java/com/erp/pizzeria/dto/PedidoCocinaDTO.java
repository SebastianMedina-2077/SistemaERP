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
    /** Minutos estimados hasta estar listo (cola del horno). null en pedidos ya ATENDIDOS. */
    private Integer tiempoEstimadoMin;
    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Item {
        private Integer idDetalle;
        private String producto;
        private String categoria;
        private Integer cantidad;
        private String observacion;
        private boolean servido;
        /** Nombre del enum de tamano: "personal", "mediano" o "familiar" (en minusculas), o null. */
        private String tamanio;

        public static Item from(DetallePedido d) {
            return Item.builder()
                    .idDetalle(d.getIdDetallePedido())
                    .producto(d.getProducto() != null ? d.getProducto().getNombre() : null)
                    .categoria(d.getProducto() != null && d.getProducto().getCategoria() != null
                            ? d.getProducto().getCategoria().getNombre() : null)
                    .cantidad(d.getCantidad())
                    .observacion(d.getObservacion())
                    .servido(Boolean.TRUE.equals(d.getServido()))
                    .tamanio(d.getProducto() != null && d.getProducto().getTamanio() != null
                            ? d.getProducto().getTamanio().name() : null)
                    .build();
        }
    }

    public static PedidoCocinaDTO from(Pedido pedido, List<DetallePedido> detalles, Integer tiempoEstimadoMin) {
        return PedidoCocinaDTO.builder()
                .idPedido(pedido.getIdPedido())
                .cliente(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null)
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado() != null ? pedido.getEstado().name() : null)
                .tiempoEstimadoMin(tiempoEstimadoMin)
                .items(detalles.stream().map(Item::from).toList())
                .build();
    }
}
