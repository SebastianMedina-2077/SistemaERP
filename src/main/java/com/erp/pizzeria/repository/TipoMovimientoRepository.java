package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Integer> {
    Optional<TipoMovimiento> findByDescripcion(String descripcion);
}
