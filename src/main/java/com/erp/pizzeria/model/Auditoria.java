package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de auditoria: una accion de escritura realizada por un usuario.
 * Guarda el username como texto (sin FK) para conservar el rastro aunque el
 * usuario sea eliminado.
 */
@Entity
@Table(name = "auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Integer idAuditoria;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario", nullable = false, length = 50)
    private String usuario;

    @Column(name = "accion", nullable = false, length = 20)
    private String accion;

    @Column(name = "entidad", nullable = false, length = 40)
    private String entidad;

    @Column(name = "referencia", length = 60)
    private String referencia;
}
