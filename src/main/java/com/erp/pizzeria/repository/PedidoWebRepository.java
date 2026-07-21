package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.PedidoWeb;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoWebRepository extends JpaRepository<PedidoWeb, Integer> {

    Optional<PedidoWeb> findByPedido_IdPedido(Integer idPedido);

    List<PedidoWeb> findByCuentaIdOrderByFechaDesc(Integer idCuenta);
}
