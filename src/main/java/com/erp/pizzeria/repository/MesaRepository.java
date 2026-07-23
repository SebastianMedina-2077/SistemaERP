package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MesaRepository extends JpaRepository<Mesa, Integer> {

    Optional<Mesa> findByNumero(Integer numero);

    List<Mesa> findAllByOrderByNumeroAsc();
}
