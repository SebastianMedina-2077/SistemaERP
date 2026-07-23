package com.erp.pizzeria.model.enums;

/**
 * Ciclo de vida de una mesa numerada del salon:
 * LIBRE -> OCUPADA (al crear un pedido con esa mesa) -> POR_LIMPIAR
 * (el cliente se retira) -> LIBRE (el cajero la marca limpia).
 */
public enum EstadoMesa {
    LIBRE,
    OCUPADA,
    POR_LIMPIAR
}
