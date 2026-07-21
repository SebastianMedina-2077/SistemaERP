package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de la validacion de codigo promocional: solo informativa para el
 * checkout, NO aplica ningun descuento. El total sigue siendo autoritativo
 * de /api/tienda/cotizar, que aplica las promociones activas automaticamente.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiendaPromoValidacionResponseDTO {

    private boolean valido;
    private String descripcion;
    private String error;

    public static TiendaPromoValidacionResponseDTO valido(String descripcion) {
        return new TiendaPromoValidacionResponseDTO(true, descripcion, null);
    }

    public static TiendaPromoValidacionResponseDTO invalido(String error) {
        return new TiendaPromoValidacionResponseDTO(false, null, error);
    }
}
