package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Insumo;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class InsumoFormDTO {

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "[A-Za-z]{2}\\d{4}", message = "Formato esperado: 2 letras + 4 digitos (ej. IN0001)")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 25, message = "Maximo 25 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "9999.99", message = "El precio excede el maximo permitido")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
    private BigDecimal stock;

    @NotNull(message = "La cantidad minima es obligatoria")
    @DecimalMin(value = "0.0", message = "La cantidad minima no puede ser negativa")
    private BigDecimal cantidadMinima;

    @NotNull(message = "Seleccione una unidad de medida")
    private Integer idMedida;

    public static InsumoFormDTO from(Insumo i) {
        InsumoFormDTO f = new InsumoFormDTO();
        f.codigo = i.getCodigo();
        f.nombre = i.getNombre();
        f.precio = i.getPrecio();
        f.stock = i.getStock();
        f.cantidadMinima = i.getCantidadMinima();
        f.idMedida = i.getMedida() != null ? i.getMedida().getIdMedida() : null;
        return f;
    }
}
