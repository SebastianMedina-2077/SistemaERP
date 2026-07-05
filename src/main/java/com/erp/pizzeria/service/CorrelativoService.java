package com.erp.pizzeria.service;

import com.erp.pizzeria.model.ComprobanteCorrelativo;
import com.erp.pizzeria.repository.ComprobanteCorrelativoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna correlativos de comprobante secuenciales y sin huecos por serie.
 */
@Service
public class CorrelativoService {

    private final ComprobanteCorrelativoRepository repository;

    public CorrelativoService(ComprobanteCorrelativoRepository repository) {
        this.repository = repository;
    }

    /**
     * Reserva y devuelve el siguiente correlativo de la serie. Debe ejecutarse dentro
     * de la transaccion de la venta: si esta hace rollback, el incremento se deshace y
     * el numero queda disponible (numeracion sin huecos).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int siguiente(String serie) {
        int filas = repository.incrementar(serie);
        if (filas == 0) {
            // Primera vez para esa serie: la crea arrancando en 1.
            repository.saveAndFlush(new ComprobanteCorrelativo(serie, 1));
            return 1;
        }
        // El UPDATE se hizo flush a BD: la lectura escalar devuelve el valor fresco
        // sin tocar el resto del contexto de persistencia (pedido, detalles, insumos).
        return repository.ultimoNumero(serie);
    }

    /**
     * Devuelve el proximo numero PREVISTO de una serie sin reservarlo (solo lectura,
     * para mostrarlo como estimacion en el modal de orden).
     */
    @Transactional(readOnly = true)
    public int proximoPrevisto(String serie) {
        return repository.findById(serie)
                .map(c -> c.getUltimoNumero() + 1)
                .orElse(1);
    }
}
