package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ProductoAdicional;
import com.erp.pizzeria.model.id.ProductoAdicionalId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoAdicionalRepository extends JpaRepository<ProductoAdicional, ProductoAdicionalId> {
    List<ProductoAdicional> findByProducto_IdProducto(Integer idProducto);
}
