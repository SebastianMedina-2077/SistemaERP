package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {
    boolean existsByUsuario_IdUsuario(Integer idUsuario);

    @Query("""
            select m from Movimiento m
            where (:tipo is null or m.tipoMovimiento.idTipoMovimiento = :tipo)
              and (:q is null or lower(m.documento) like lower(concat('%', :q, '%'))
                              or lower(m.glosa) like lower(concat('%', :q, '%')))
            """)
    Page<Movimiento> buscar(@Param("tipo") Integer tipo,
                            @Param("q") String q,
                            Pageable pageable);

    @Query("""
            select m from Movimiento m
            join fetch m.tipoMovimiento
            join fetch m.usuario u
            left join fetch u.empleado
            where m.idMovimiento = :idMovimiento
            """)
    Optional<Movimiento> findComprobanteById(@Param("idMovimiento") Integer idMovimiento);
}
