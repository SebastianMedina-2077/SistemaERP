package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detallepedido")
    private Integer idDetallePedido;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 8, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 8, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "descuento", precision = 8, scale = 2)
    private BigDecimal descuento;

    @Column(name = "observacion", length = 100)
    private String observacion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;
}
