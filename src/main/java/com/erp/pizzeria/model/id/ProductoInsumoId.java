package com.erp.pizzeria.model.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductoInsumoId implements Serializable {

    @Column(name = "id_insumo")
    private Integer idInsumo;

    @Column(name = "id_producto")
    private Integer idProducto;
}
