package com.erp.pizzeria.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }

    public static ResourceNotFoundException of(String entidad, Object id) {
        return new ResourceNotFoundException(entidad + " no encontrado: " + id);
    }
}
