package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Adicional que un producto admite (extra queso, salsa, etc.) y que suma precio.
 * El precio lo pone el servidor al cotizar; el cliente solo elige id y cantidad.
 */
@Entity
@Table(name = "adicional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Adicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adicional")
    private Integer idAdicional;

    @Column(name = "nombre", length = 40, nullable = false)
    private String nombre;

    @Column(name = "precio", precision = 6, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "disponible", nullable = false)
    private Boolean disponible;

    @Column(name = "grupo", length = 20)
    private String grupo;
}
