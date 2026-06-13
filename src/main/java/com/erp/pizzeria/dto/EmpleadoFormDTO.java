package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Empleado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmpleadoFormDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 20, message = "Maximo 20 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 20, message = "Maximo 20 caracteres")
    private String apellido;

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 digitos")
    private String dni;

    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;

    @NotBlank(message = "El cargo es obligatorio")
    @Size(max = 15, message = "Maximo 15 caracteres")
    private String cargo;

    public static EmpleadoFormDTO from(Empleado e) {
        EmpleadoFormDTO f = new EmpleadoFormDTO();
        f.nombre = e.getNombre();
        f.apellido = e.getApellido();
        f.dni = e.getDni();
        f.telefono = e.getTelefono();
        f.cargo = e.getCargo();
        return f;
    }

    public void applyTo(Empleado e) {
        e.setNombre(nombre.trim());
        e.setApellido(apellido.trim());
        e.setDni(dni);
        e.setTelefono(telefono);
        e.setCargo(cargo.trim());
    }
}
