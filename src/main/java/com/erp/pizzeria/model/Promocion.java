package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "promocion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_promocion")
    private Integer idPromocion;

    @Column(name = "nombre", length = 25)
    private String nombre;

    @Column(name = "descripcion", length = 25)
    private String descripcion;

    @Column(name = "tipo_descuento", length = 20)
    private String tipoDescuento;

    @Column(name = "valor_descuento", precision = 8, scale = 2)
    private BigDecimal valorDescuento;

    @Column(name = "activa")
    private Boolean activa;
}
