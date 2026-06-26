package com.erp.pizzeria.model;

import com.erp.pizzeria.model.enums.EstadoInsumo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "insumo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_insumo")
    private Integer idInsumo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo", length = 6, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 25, nullable = false)
    private String nombre;

    @Column(name = "precio", precision = 6, scale = 2, nullable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoInsumo estado;

    @Column(name = "stock", precision = 8, scale = 3, nullable = false)
    private BigDecimal stock;

    @Column(name = "cantidad_minima", precision = 8, scale = 3, nullable = false)
    private BigDecimal cantidadMinima;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_medida", nullable = false)
    private Medida medida;

    /** Estado derivado del stock: bajo cuando llega al minimo o por debajo, normal en otro caso. */
    public void recalcularEstado() {
        this.estado = stock.compareTo(cantidadMinima) <= 0 ? EstadoInsumo.bajo : EstadoInsumo.normal;
    }
}
