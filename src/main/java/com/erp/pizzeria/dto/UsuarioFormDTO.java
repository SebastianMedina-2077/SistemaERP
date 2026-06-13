package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Usuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioFormDTO {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 10, message = "Maximo 10 caracteres")
    @Pattern(regexp = "[a-zA-Z0-9_]+", message = "Solo letras, numeros y guion bajo")
    private String username;

    private String password;

    @NotNull(message = "Seleccione un rol")
    private Integer idRol;

    private Integer idEmpleado;

    private boolean estado = true;

    public static UsuarioFormDTO from(Usuario u) {
        UsuarioFormDTO f = new UsuarioFormDTO();
        f.username = u.getUsername();
        f.idRol = u.getRol() != null ? u.getRol().getIdRol() : null;
        f.idEmpleado = u.getEmpleado() != null ? u.getEmpleado().getIdEmpleado() : null;
        f.estado = Boolean.TRUE.equals(u.getEstado());
        return f;
    }
}
