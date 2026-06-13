package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_movimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detallemovimiento")
    private Integer idDetalleMovimiento;

    @Column(name = "cantidad", precision = 8, scale = 3, nullable = false)
    private BigDecimal cantidad;

    @Column(name = "stock_resultante", precision = 8, scale = 3)
    private BigDecimal stockResultante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_insumo", nullable = false)
    private Insumo insumo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_movimiento", nullable = false)
    private Movimiento movimiento;
}
