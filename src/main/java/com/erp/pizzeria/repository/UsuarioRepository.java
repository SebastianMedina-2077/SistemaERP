package com.erp.pizzeria.repository;

import com.erp.pizzeria.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);

    // Trae rol y empleado en una sola query; lo usan el login y CurrentUserAdvice (corre en cada request).
    @Query("select u from Usuario u join fetch u.rol left join fetch u.empleado where u.username = :username")
    Optional<Usuario> findByUsernameConRelaciones(@Param("username") String username);

    // Listado de usuarios sin N+1: rol y empleado vienen en el mismo select.
    @Query("select u from Usuario u join fetch u.rol left join fetch u.empleado order by u.idUsuario")
    List<Usuario> findAllConRelaciones();

    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCaseAndIdUsuarioNot(String username, Integer idUsuario);
    boolean existsByEmpleado_IdEmpleado(Integer idEmpleado);
}
