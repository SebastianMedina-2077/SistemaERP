package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ComprobanteCorrelativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteCorrelativoRepository extends JpaRepository<ComprobanteCorrelativo, String> {

    /**
     * Incrementa atomicamente el contador de la serie. El UPDATE toma un lock de
     * fila que serializa a los cajeros concurrentes, evitando numeros duplicados.
     * flushAutomatically asegura que el cambio llegue a la BD antes de releerlo;
     * clearAutomatically=false para NO desasociar el resto de entidades de la venta.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = false)
    @Query("UPDATE ComprobanteCorrelativo c SET c.ultimoNumero = c.ultimoNumero + 1 WHERE c.serie = :serie")
    int incrementar(@Param("serie") String serie);

    /** Lectura escalar del contador (consulta a BD, valor fresco dentro de la transaccion). */
    @Query("SELECT c.ultimoNumero FROM ComprobanteCorrelativo c WHERE c.serie = :serie")
    Integer ultimoNumero(@Param("serie") String serie);
}
