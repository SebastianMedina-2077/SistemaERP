package com.erp.pizzeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Cuerpo de POST /api/tienda/cuenta/verificar: el codigo de 6 digitos del correo. */
public class VerificarCorreoDTO {

    @NotBlank(message = "Ingresa el codigo de verificacion")
    @Pattern(regexp = "\\d{6}", message = "El codigo debe tener 6 digitos")
    private String codigo;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
