package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cotizacion para la tienda en linea. Es una proyeccion de {@link CotizacionDTO}
 * (misma logica autoritativa del POS) con la forma que espera el frontend web:
 * cada linea lleva la descripcion del producto y el subtotal ya con descuento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiendaCotizacionDTO {

    private List<Linea> lineas;
    /** Total autoritativo calculado en backend (IGV incluido). */
    private BigDecimal total;
    /** true si el codigo enviado existe y esta activo (su descuento ya va en lineas/total). */
    private boolean cuponAplicado;
    /** Descripcion del cupon aplicado; null si no se aplico ninguno. */
    private String descripcionCupon;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Linea {
        private Integer idProducto;
        private String descripcion;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal descuento;
        private BigDecimal subtotal;
    }
}
