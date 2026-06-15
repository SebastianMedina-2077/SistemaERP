package com.erp.pizzeria.util;

/**
 * Generador de codigos secuenciales con prefijo (ej. IN0001, PZ0010).
 * El formato es: prefijo de letras + 4 digitos con ceros a la izquierda,
 * lo que coincide con la columna {@code codigo char(6)} de insumo y producto.
 */
public final class CodigoUtil {

    private CodigoUtil() {
    }

    /**
     * Calcula el siguiente codigo a partir del ultimo registrado con ese prefijo.
     *
     * @param prefijo      prefijo de letras (ej. "IN", "PZ")
     * @param ultimoCodigo el ultimo codigo existente con ese prefijo, o {@code null} si no hay ninguno
     * @return el siguiente codigo (ej. "IN0001" cuando no hay previos)
     */
    public static String siguiente(String prefijo, String ultimoCodigo) {
        int numero = 0;
        if (ultimoCodigo != null && ultimoCodigo.length() > prefijo.length()) {
            try {
                numero = Integer.parseInt(ultimoCodigo.substring(prefijo.length()));
            } catch (NumberFormatException ignored) {
                numero = 0;
            }
        }
        return prefijo + String.format("%04d", numero + 1);
    }
}