package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.DetalleMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleMovimientoRepository extends JpaRepository<DetalleMovimiento, Integer> {
    List<DetalleMovimiento> findByInsumo_IdInsumo(Integer idInsumo);
    List<DetalleMovimiento> findByMovimiento_IdMovimiento(Integer idMovimiento);
    boolean existsByInsumo_IdInsumo(Integer idInsumo);
}
