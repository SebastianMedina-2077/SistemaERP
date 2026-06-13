package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "nombre", length = 20, nullable = false)
    private String nombre;

    @Column(name = "apellido", length = 20, nullable = false)
    private String apellido;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "dni", length = 8, nullable = false, unique = true)
    private String dni;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "telefono", length = 9, nullable = false)
    private String telefono;

    @Column(name = "cargo", length = 15, nullable = false)
    private String cargo;
}
