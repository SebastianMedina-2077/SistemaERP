package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {
    boolean existsByDni(String dni);
    boolean existsByDniAndIdEmpleadoNot(String dni, Integer idEmpleado);
}
