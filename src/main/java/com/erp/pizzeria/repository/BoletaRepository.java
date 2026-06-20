package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoletaRepository extends JpaRepository<Boleta, Integer> {
    Optional<Boleta> findByPedido_IdPedido(Integer idPedido);
    List<Boleta> findByPedido_IdPedidoIn(Collection<Integer> idsPedido);
}
