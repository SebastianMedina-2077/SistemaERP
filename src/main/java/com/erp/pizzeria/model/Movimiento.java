package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "movimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @Column(name = "documento", length = 15)
    private String documento;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "glosa", length = 25)
    private String glosa;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipomovimiento", nullable = false)
    private TipoMovimiento tipoMovimiento;

    @ManyToOne
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;
}
