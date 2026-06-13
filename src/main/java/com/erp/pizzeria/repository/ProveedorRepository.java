package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
    boolean existsByRuc(String ruc);
    boolean existsByRucAndIdProveedorNot(String ruc, Integer idProveedor);
}
