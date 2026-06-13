package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proveedor")
    private Integer idProveedor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ruc", length = 11, nullable = false)
    private String ruc;

    @Column(name = "nombre", length = 25, nullable = false)
    private String nombre;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "telefono", length = 9, nullable = false)
    private String telefono;

    @Column(name = "direccion", length = 25, nullable = false)
    private String direccion;
}
