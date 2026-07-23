package com.erp.pizzeria.dto;

/**
 * Vista de una mesa para la caja: numero, capacidad y estado
 * (name() del enum EstadoMesa: LIBRE / OCUPADA / POR_LIMPIAR).
 */
public record MesaDTO(Integer numero, Integer capacidad, String estado) {
}
