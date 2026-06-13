package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ProductoInsumo;
import com.erp.pizzeria.model.id.ProductoInsumoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoInsumoRepository extends JpaRepository<ProductoInsumo, ProductoInsumoId> {
    List<ProductoInsumo> findByProducto_IdProducto(Integer idProducto);
    List<ProductoInsumo> findByInsumo_IdInsumo(Integer idInsumo);
    boolean existsByInsumo_IdInsumo(Integer idInsumo);
}
