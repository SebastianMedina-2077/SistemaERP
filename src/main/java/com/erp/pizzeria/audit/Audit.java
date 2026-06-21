package com.erp.pizzeria.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un metodo de servicio para que {@link AuditAspect} registre la accion
 * en la tabla de auditoria cuando termina con exito.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** Accion realizada, p. ej. CREAR, EDITAR, ELIMINAR, ANULAR, ESTADO. */
    String accion();

    /** Entidad afectada, p. ej. Producto, Pedido, Usuario. */
    String entidad();
}
