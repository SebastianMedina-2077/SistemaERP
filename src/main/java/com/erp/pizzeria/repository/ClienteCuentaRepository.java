package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ClienteCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteCuentaRepository extends JpaRepository<ClienteCuenta, Integer> {

    Optional<ClienteCuenta> findByEmail(String email);

    boolean existsByEmail(String email);
}
