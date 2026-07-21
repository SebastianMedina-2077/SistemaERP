package com.erp.pizzeria.model;

import com.erp.pizzeria.model.id.ProductoAdicionalId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Adicionales que admite cada producto (que el cliente puede elegir). */
@Entity
@Table(name = "producto_adicional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoAdicional {

    @EmbeddedId
    private ProductoAdicionalId id;

    @ManyToOne(optional = false)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false)
    @MapsId("idAdicional")
    @JoinColumn(name = "id_adicional", nullable = false)
    private Adicional adicional;
}
