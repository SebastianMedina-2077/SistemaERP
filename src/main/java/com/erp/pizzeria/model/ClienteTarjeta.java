package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tarjeta guardada de un cliente web. Solo marca + ultimos 4 + titular; JAMAS el numero completo ni el CVV.
 */
@Entity
@Table(name = "cliente_tarjeta")
@Getter
@Setter
@NoArgsConstructor
public class ClienteTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cuenta", nullable = false)
    private ClienteCuenta cuenta;

    @Column(name = "marca", length = 20, nullable = false)
    private String marca;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "ultimos4", length = 4, nullable = false)
    private String ultimos4;

    @Column(name = "titular", length = 80, nullable = false)
    private String titular;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
