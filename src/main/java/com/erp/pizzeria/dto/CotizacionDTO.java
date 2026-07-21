package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Total autoritativo de un pedido (preview de precios), calculado con la MISMA
 * logica que persiste {@code PedidoService.crearPedido}: descuentos de promocion
 * por linea e IGV incluido (igv = total x 18/118). No verifica stock ni persiste.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionDTO {

    /** Base imponible: total - igv. */
    private BigDecimal subtotal;
    /** IGV extraido del total (los precios ya incluyen IGV). */
    private BigDecimal igv;
    /** Total autoritativo: suma de los subtotales de linea. */
    private BigDecimal total;
    private List<Linea> lineas;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Linea {
        private Integer idProducto;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal descuento;
        private BigDecimal subtotalLinea;
        private List<LineaAdicional> adicionales;
    }

    /** Adicional resuelto en una linea (precio puesto por el servidor). */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineaAdicional {
        private Integer idAdicional;
        private String nombre;
        private BigDecimal precioUnitario;
        private Integer cantidad;
    }
}
