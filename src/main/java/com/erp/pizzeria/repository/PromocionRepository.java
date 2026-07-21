package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Integer> {
    List<Promocion> findByActivaTrue();

    Optional<Promocion> findByCodigoAndActivaTrue(String codigo);
}
