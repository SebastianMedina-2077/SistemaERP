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
public class ComboProductoId implements Serializable {

    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "id_combo")
    private Integer idCombo;
}
