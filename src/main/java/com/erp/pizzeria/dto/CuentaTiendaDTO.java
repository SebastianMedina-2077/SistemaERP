package com.erp.pizzeria.dto;

import java.util.List;

/** Perfil de la cuenta autenticada de la tienda web. */
public record CuentaTiendaDTO(String nombre, String email, String telefono, List<TarjetaTiendaDTO> tarjetas) {
}
