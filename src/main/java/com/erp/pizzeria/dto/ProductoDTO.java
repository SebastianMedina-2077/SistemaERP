package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoDTO {
    private Integer idProducto;
    private String codigo;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponible;
    private Integer idCategoria;
    private String categoria;
    private boolean preparado;

    public static ProductoDTO from(Producto p) {
        return ProductoDTO.builder()
                .idProducto(p.getIdProducto())
                .codigo(p.getCodigo())
                .nombre(p.getNombre())
                .precio(p.getPrecio())
                .stock(p.getStock())
                .disponible(p.getDisponible())
                .idCategoria(p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null)
                .categoria(p.getCategoria() != null ? p.getCategoria().getNombre() : null)
                .preparado(p.getStock() == null)
                .build();
    }
}
