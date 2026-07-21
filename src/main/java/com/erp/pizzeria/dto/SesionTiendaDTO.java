package com.erp.pizzeria.dto;

/** Respuesta de registro/login de la tienda web: JWT + identidad basica. */
public record SesionTiendaDTO(String token, String nombre, String email) {
}
