package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ClienteTarjeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteTarjetaRepository extends JpaRepository<ClienteTarjeta, Integer> {

    List<ClienteTarjeta> findByCuentaIdOrderByIdAsc(Integer idCuenta);
}
