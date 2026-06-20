package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.DetalleMovimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetalleMovimientoRepository extends JpaRepository<DetalleMovimiento, Integer> {
    List<DetalleMovimiento> findByInsumo_IdInsumo(Integer idInsumo);
    List<DetalleMovimiento> findByMovimiento_IdMovimiento(Integer idMovimiento);
    boolean existsByInsumo_IdInsumo(Integer idInsumo);

    @Query("""
            select d from DetalleMovimiento d
            where (:q is null or lower(d.insumo.nombre) like lower(concat('%', :q, '%')))
            """)
    Page<DetalleMovimiento> buscar(@Param("q") String q, Pageable pageable);
}
