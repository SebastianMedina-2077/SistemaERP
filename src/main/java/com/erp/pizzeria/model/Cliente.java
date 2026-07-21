package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @Column(name = "nombre", length = 40, nullable = false)
    private String nombre;

    @Column(name = "apellido_paterno", length = 40)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", length = 40)
    private String apellidoMaterno;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "telefono", length = 9)
    private String telefono;

    // Ubicacion del cliente (la registra la tienda web; los clientes del POS pueden no tenerla)
    @Column(name = "departamento", length = 60)
    private String departamento;

    @Column(name = "provincia", length = 60)
    private String provincia;

    @Column(name = "distrito", length = 60)
    private String distrito;

    @Column(name = "direccion_exacta", length = 50)
    private String direccionExacta;

    @Column(name = "referencia", length = 50)
    private String referencia;
}
