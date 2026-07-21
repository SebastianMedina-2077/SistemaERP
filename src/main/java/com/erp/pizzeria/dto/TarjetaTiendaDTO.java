package com.erp.pizzeria.dto;

/** Tarjeta guardada del cliente web: solo datos no sensibles. */
public record TarjetaTiendaDTO(Integer id, String marca, String ultimos4, String titular) {
}
