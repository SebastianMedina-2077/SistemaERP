package com.erp.pizzeria.model;

import com.erp.pizzeria.model.id.ProductoInsumoId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "producto_insumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoInsumo {

    @EmbeddedId
    private ProductoInsumoId id;

    @Column(name = "cantidad", precision = 6, scale = 3, nullable = false)
    private BigDecimal cantidad;

    @ManyToOne(optional = false)
    @MapsId("idInsumo")
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    @ManyToOne(optional = false)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;
}
