package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Insumo;
import com.erp.pizzeria.model.enums.EstadoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Integer> {
    List<Insumo> findByEstado(EstadoInsumo estado);
    Optional<Insumo> findByCodigo(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdInsumoNot(String codigo, Integer idInsumo);
}
