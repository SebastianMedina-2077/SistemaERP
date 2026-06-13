package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Pedido;
import com.erp.pizzeria.model.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEstado(EstadoPedido estado);
    List<Pedido> findByEstadoInOrderByFechaAsc(List<EstadoPedido> estados);
    boolean existsByUsuario_IdUsuario(Integer idUsuario);
}
