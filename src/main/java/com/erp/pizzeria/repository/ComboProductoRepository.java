package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ComboProducto;
import com.erp.pizzeria.model.id.ComboProductoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComboProductoRepository extends JpaRepository<ComboProducto, ComboProductoId> {
    List<ComboProducto> findByCombo_IdProducto(Integer idCombo);
    boolean existsByProducto_IdProducto(Integer idProducto);
}
