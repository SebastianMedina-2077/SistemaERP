package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medida")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Medida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medida")
    private Integer idMedida;

    @Column(name = "descripcion", length = 15, nullable = false)
    private String descripcion;

    @Column(name = "sigla", length = 4, nullable = false)
    private String sigla;
}
