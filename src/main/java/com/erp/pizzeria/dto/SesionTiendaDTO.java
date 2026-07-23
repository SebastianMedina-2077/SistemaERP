package com.erp.pizzeria.dto;

/**
 * Respuesta de registro/login de la tienda web: JWT + identidad basica.
 * {@code emailVerificado} indica si el cliente ya confirmo su correo con el codigo
 * (verificacion blanda: no bloquea el login).
 */
public record SesionTiendaDTO(String token, String nombre, String email, boolean emailVerificado) {
}
