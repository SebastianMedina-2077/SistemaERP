package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoGuardadoDTO {

    private Long id;
    private String referencia;
    private String cliente;
    private String telefono;
    private Integer idMetodoPago;
    private List<ItemGuardadoDTO> items;
    private BigDecimal subtotal;
    private BigDecimal total;
    private LocalDateTime fechaGuardado;

    public int getCantidadItems() {
        if (items == null) return 0;
        return items.stream().mapToInt(i -> i.getCantidad() == null ? 0 : i.getCantidad()).sum();
    }

    public String getHora() {
        return fechaGuardado == null ? "" : fechaGuardado.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
