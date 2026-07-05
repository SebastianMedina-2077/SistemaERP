package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cliente con RUC (empresa) para la emision de facturas. Permite autocompletar la
 * razon social cuando el RUC ya fue registrado en una venta anterior.
 */
@Entity
@Table(name = "cliente_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente_empresa")
    private Integer idClienteEmpresa;

    @Column(name = "ruc", length = 11, nullable = false, unique = true)
    private String ruc;

    @Column(name = "razon_social", length = 120, nullable = false)
    private String razonSocial;
}
