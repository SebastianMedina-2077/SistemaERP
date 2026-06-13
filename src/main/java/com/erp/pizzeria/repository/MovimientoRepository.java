package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {
    boolean existsByUsuario_IdUsuario(Integer idUsuario);
}
