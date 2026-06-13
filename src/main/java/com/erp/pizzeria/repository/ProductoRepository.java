package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByDisponibleTrue();
    List<Producto> findByCategoria_IdCategoria(Integer idCategoria);
    List<Producto> findByDisponibleTrueAndCategoria_IdCategoria(Integer idCategoria);
    Optional<Producto> findByCodigo(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdProductoNot(String codigo, Integer idProducto);
}
