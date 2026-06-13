package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Integer> {
    List<Compra> findByProveedor_IdProveedor(Integer idProveedor);
    boolean existsByProveedor_IdProveedor(Integer idProveedor);
    boolean existsByUsuario_IdUsuario(Integer idUsuario);
}
