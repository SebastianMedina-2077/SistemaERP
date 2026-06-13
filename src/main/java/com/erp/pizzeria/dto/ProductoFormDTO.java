package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Producto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class ProductoFormDTO {

    @NotBlank(message = "El codigo es obligatorio")
    @Pattern(regexp = "[A-Za-z]{2}\\d{4}", message = "Formato esperado: 2 letras + 4 digitos (ej. PZ0001)")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "Maximo 60 caracteres")
    private String nombre;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    @DecimalMax(value = "9999.99", message = "El precio excede el maximo permitido")
    private BigDecimal precio;

    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stock;

    private String tamanio;

    private boolean disponible = true;

    @NotNull(message = "Seleccione una categoria")
    private Integer idCategoria;

    public static ProductoFormDTO from(Producto p) {
        ProductoFormDTO f = new ProductoFormDTO();
        f.codigo = p.getCodigo();
        f.nombre = p.getNombre();
        f.precio = p.getPrecio();
        f.stock = p.getStock();
        f.tamanio = p.getTamanio() != null ? p.getTamanio().name() : null;
        f.disponible = Boolean.TRUE.equals(p.getDisponible());
        f.idCategoria = p.getCategoria() != null ? p.getCategoria().getIdCategoria() : null;
        return f;
    }
}
