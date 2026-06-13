package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detallecompra")
    private Integer idDetalleCompra;

    @Column(name = "cantidad", precision = 8, scale = 3, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", precision = 6, scale = 2, nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", precision = 6, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;
}
