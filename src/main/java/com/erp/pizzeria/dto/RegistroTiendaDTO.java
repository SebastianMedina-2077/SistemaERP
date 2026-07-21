package com.erp.pizzeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroTiendaDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 40, message = "Maximo 40 caracteres")
    private String nombres;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 40, message = "Maximo 40 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(max = 40, message = "Maximo 40 caracteres")
    private String apellidoMaterno;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es valido")
    @Size(max = 120, message = "Maximo 120 caracteres")
    private String email;

    @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    // Contrasena fuerte solo para cuentas nuevas de la tienda: mayuscula, minuscula, numero y caracter especial.
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "La contrasena debe tener 8+ caracteres con mayuscula, minuscula, numero y caracter especial")
    private String password;

    // Ubicacion (Peru): departamento/provincia/distrito por combos + direccion libre.
    @NotBlank(message = "Selecciona el departamento")
    @Size(max = 60, message = "Maximo 60 caracteres")
    private String departamento;

    @NotBlank(message = "Selecciona la provincia")
    @Size(max = 60, message = "Maximo 60 caracteres")
    private String provincia;

    @NotBlank(message = "Selecciona el distrito")
    @Size(max = 60, message = "Maximo 60 caracteres")
    private String distrito;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 50, message = "Maximo 50 caracteres")
    private String direccionExacta;

    @Size(max = 50, message = "Maximo 50 caracteres")
    private String referencia;
}
