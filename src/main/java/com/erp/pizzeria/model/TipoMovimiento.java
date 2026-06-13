package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tipo_movimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipomovimiento")
    private Integer idTipoMovimiento;

    @Column(name = "descripcion", length = 15, nullable = false)
    private String descripcion;

    @Column(name = "operacion", length = 15, nullable = false)
    private String operacion;
}
