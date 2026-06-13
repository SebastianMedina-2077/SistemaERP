package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromocionRepository extends JpaRepository<Promocion, Integer> {
    List<Promocion> findByActivaTrue();
}
