package com.erp.pizzeria.dto;

import java.math.BigDecimal;
import java.util.List;

/** Detalle de un producto para la pagina de personalizacion: datos + adicionales admitidos. */
public record TiendaProductoDetalleDTO(
        Integer id,
        String nombre,
        BigDecimal precio,
        String categoria,
        String imagenUrl,
        List<AdicionalItem> adicionales) {

    public record AdicionalItem(Integer idAdicional, String nombre, BigDecimal precio, String grupo) {}
}
