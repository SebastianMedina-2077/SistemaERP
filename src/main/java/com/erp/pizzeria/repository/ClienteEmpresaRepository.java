package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.ClienteEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteEmpresaRepository extends JpaRepository<ClienteEmpresa, Integer> {
    Optional<ClienteEmpresa> findByRuc(String ruc);
}
