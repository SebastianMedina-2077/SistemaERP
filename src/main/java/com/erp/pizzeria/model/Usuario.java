package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "username", length = 10, nullable = false)
    private String username;

    @Column(name = "password", length = 60, nullable = false)
    private String password;

    @Column(name = "estado", nullable = false)
    private Boolean estado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;
}
