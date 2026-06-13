package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Integer> {
    List<DetalleCompra> findByCompra_IdCompra(Integer idCompra);
    boolean existsByInsumo_IdInsumo(Integer idInsumo);
}
