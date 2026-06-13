package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {
    List<MetodoPago> findByActivoTrue();
}
