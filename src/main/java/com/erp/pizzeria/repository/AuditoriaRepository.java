package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {

    List<Auditoria> findTop500ByOrderByFechaDescIdAuditoriaDesc();
}
