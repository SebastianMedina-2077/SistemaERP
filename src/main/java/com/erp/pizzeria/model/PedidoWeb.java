package com.erp.pizzeria.model;

import com.erp.pizzeria.model.enums.ModalidadEntrega;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Detalle web de un pedido hecho desde la tienda en linea: cuenta que lo genero
 * y modalidad de entrega. La venta en si vive en {@link Pedido} (mismo flujo del POS).
 */
@Entity
@Table(name = "pedido_web")
@Getter
@Setter
@NoArgsConstructor
public class PedidoWeb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cuenta", nullable = false)
    private ClienteCuenta cuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    private ModalidadEntrega modalidad;

    /** Solo para DELIVERY; en RECOJO queda null. */
    @Column(name = "direccion", length = 150)
    private String direccion;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
