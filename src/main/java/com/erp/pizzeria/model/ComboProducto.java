package com.erp.pizzeria.model;

import com.erp.pizzeria.model.id.ComboProductoId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "combo_producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComboProducto {

    @EmbeddedId
    private ComboProductoId id;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @ManyToOne(optional = false)
    @MapsId("idProducto")
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false)
    @MapsId("idCombo")
    @JoinColumn(name = "id_combo", nullable = false)
    private Producto combo;
}
