package com.erp.pizzeria.audit;

import com.erp.pizzeria.service.AuditoriaService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Registra en auditoria cada metodo anotado con {@link Audit} que termina con
 * exito. El registro corre en una transaccion propia (REQUIRES_NEW) y cualquier
 * fallo de auditoria se traga para no afectar la operacion de negocio.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditoriaService auditoriaService;

    public AuditAspect(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @AfterReturning(pointcut = "@annotation(audit)", returning = "resultado")
    public void registrar(JoinPoint joinPoint, Audit audit, Object resultado) {
        try {
            String referencia = resolverReferencia(resultado, joinPoint.getArgs());
            auditoriaService.registrar(audit.accion(), audit.entidad(), referencia);
        } catch (RuntimeException ex) {
            log.warn("No se pudo registrar auditoria de {} {}: {}",
                    audit.accion(), audit.entidad(), ex.getMessage());
        }
    }

    /**
     * Deriva una referencia legible (#id). Prefiere el id del valor devuelto
     * (cubre crear/editar que retornan la entidad) y, si el metodo es void
     * (eliminar), usa el primer argumento entero (el id recibido).
     */
    private String resolverReferencia(Object resultado, Object[] args) {
        Integer id = idDe(resultado);
        if (id != null) {
            return "#" + id;
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Integer entero) {
                    return "#" + entero;
                }
            }
        }
        return null;
    }

    private Integer idDe(Object objeto) {
        if (objeto == null) {
            return null;
        }
        for (Method m : objeto.getClass().getMethods()) {
            if (m.getParameterCount() == 0
                    && m.getName().startsWith("getId")
                    && Number.class.isAssignableFrom(boxear(m.getReturnType()))) {
                try {
                    Object valor = m.invoke(objeto);
                    return valor != null ? ((Number) valor).intValue() : null;
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Class<?> boxear(Class<?> tipo) {
        if (tipo == int.class) return Integer.class;
        if (tipo == long.class) return Long.class;
        if (tipo == short.class) return Short.class;
        return tipo;
    }
}
