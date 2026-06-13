package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Promocion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PromocionFormDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 25, message = "Maximo 25 caracteres")
    private String nombre;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 25, message = "Maximo 25 caracteres")
    private String descripcion;

    @NotNull(message = "Seleccione el tipo de descuento")
    @Pattern(regexp = "Porcentaje|Monto", message = "Tipo de descuento invalido")
    private String tipoDescuento;

    @NotNull(message = "El valor es obligatorio")
    @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
    private BigDecimal valorDescuento;

    private boolean activa = true;

    private List<Integer> productoIds = new ArrayList<>();

    public static PromocionFormDTO from(Promocion p, List<Integer> productoIds) {
        PromocionFormDTO f = new PromocionFormDTO();
        f.nombre = p.getNombre();
        f.descripcion = p.getDescripcion();
        f.tipoDescuento = p.getTipoDescuento();
        f.valorDescuento = p.getValorDescuento();
        f.activa = Boolean.TRUE.equals(p.getActiva());
        f.productoIds = productoIds;
        return f;
    }
}
