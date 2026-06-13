package com.erp.pizzeria.model;

import com.erp.pizzeria.model.enums.Tamanio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo", length = 6, nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 60, nullable = false)
    private String nombre;

    @Column(name = "precio", precision = 6, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "stock")
    private Integer stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "tamanio")
    private Tamanio tamanio;

    @Column(name = "disponible", nullable = false)
    private Boolean disponible;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;
}
