package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Integer> {
    List<DetallePedido> findByPedido_IdPedido(Integer idPedido);
    boolean existsByProducto_IdProducto(Integer idProducto);
}
