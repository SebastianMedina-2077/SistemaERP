package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Parte de pago de un pedido. Un pedido puede tener uno o varios pagos
 * (pago mixto: ej. parte en efectivo y parte con tarjeta/yape).
 */
@Entity
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_metodopago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto", precision = 8, scale = 2, nullable = false)
    private BigDecimal monto;
}
