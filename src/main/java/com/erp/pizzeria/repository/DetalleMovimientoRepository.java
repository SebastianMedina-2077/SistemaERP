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
            join fetch d.insumo i
            join fetch i.medida
            where d.movimiento.idMovimiento = :idMovimiento
            order by d.idDetalleMovimiento
            """)
    List<DetalleMovimiento> findComprobanteLineas(@Param("idMovimiento") Integer idMovimiento);

    @Query("""
            select d from DetalleMovimiento d
            where (:q is null or lower(d.insumo.nombre) like lower(concat('%', :q, '%')))
            """)
    Page<DetalleMovimiento> buscar(@Param("q") String q, Pageable pageable);
}
