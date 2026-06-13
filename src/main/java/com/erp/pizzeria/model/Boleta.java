package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "boleta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleta")
    private Integer idBoleta;

    @Column(name = "subtotal", precision = 6, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "igv", precision = 8, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", precision = 8, scale = 2, nullable = false)
    private BigDecimal total;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_metodopago", nullable = false)
    private MetodoPago metodoPago;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;
}
