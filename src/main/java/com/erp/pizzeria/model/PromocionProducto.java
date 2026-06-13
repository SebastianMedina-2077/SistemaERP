package com.erp.pizzeria.model;

import com.erp.pizzeria.model.id.PromocionProductoId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promocion_producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromocionProducto {

    @EmbeddedId
    private PromocionProductoId id;

    @Column(name = "cantidad_minima", nullable = false)
    private Integer cantidadMinima;

    @ManyToOne(optional = false)
    @MapsId("idPromocion")
    @JoinColumn(name = "id_promocion", nullable = false)
    private Promocion promocion;

    @ManyToOne(optional = false)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;
}
