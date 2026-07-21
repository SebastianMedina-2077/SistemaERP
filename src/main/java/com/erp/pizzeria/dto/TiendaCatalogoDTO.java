package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Catalogo publico de la tienda en linea: solo productos disponibles y
 * promociones activas, con los nombres de campo que consume static/js/tienda.js.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiendaCatalogoDTO {

    private List<CategoriaItem> categorias;
    private List<ProductoItem> productos;
    private List<PromocionItem> promociones;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaItem {
        private Integer id;
        private String nombre;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductoItem {
        private Integer id;
        private String codigo;
        private String nombre;
        private BigDecimal precio;
        private Integer idCategoria;
        private String categoria;
        private String imagenUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromocionItem {
        private Integer id;
        private String descripcion;
    }
}
