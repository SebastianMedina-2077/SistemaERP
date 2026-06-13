package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
}
