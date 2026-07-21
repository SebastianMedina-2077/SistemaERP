package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Adicional elegido en una linea del pedido. {@code precioUnitario} congela el
 * precio de venta del adicional al momento de la venta (como en detalle_pedido).
 */
@Entity
@Table(name = "detalle_pedido_adicional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetallePedidoAdicional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", precision = 6, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_detallepedido", nullable = false)
    private DetallePedido detallePedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_adicional", nullable = false)
    private Adicional adicional;
}
