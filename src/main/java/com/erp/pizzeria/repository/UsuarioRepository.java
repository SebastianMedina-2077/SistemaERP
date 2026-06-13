package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndIdUsuarioNot(String username, Integer idUsuario);
    boolean existsByEmpleado_IdEmpleado(Integer idEmpleado);
}
