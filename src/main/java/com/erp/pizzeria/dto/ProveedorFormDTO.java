package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Proveedor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProveedorFormDTO {

    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 digitos")
    private String ruc;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 25, message = "Maximo 25 caracteres")
    private String nombre;

    @NotBlank(message = "El telefono es obligatorio")
    @Pattern(regexp = "\\d{9}", message = "El telefono debe tener 9 digitos")
    private String telefono;

    @NotBlank(message = "La direccion es obligatoria")
    @Size(max = 25, message = "Maximo 25 caracteres")
    private String direccion;

    public static ProveedorFormDTO from(Proveedor p) {
        ProveedorFormDTO f = new ProveedorFormDTO();
        f.ruc = p.getRuc();
        f.nombre = p.getNombre();
        f.telefono = p.getTelefono();
        f.direccion = p.getDireccion();
        return f;
    }

    public void applyTo(Proveedor p) {
        p.setRuc(ruc);
        p.setNombre(nombre);
        p.setTelefono(telefono);
        p.setDireccion(direccion);
    }
}
