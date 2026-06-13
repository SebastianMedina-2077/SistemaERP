package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.Boleta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletaDTO {
    private Integer idBoleta;
    private Integer idPedido;
    private String numeroBoleta;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;

    public static String numeroDe(Integer idBoleta) {
        return idBoleta == null ? "B001-000000" : String.format("B001-%06d", idBoleta);
    }

    public static BoletaDTO from(Boleta b) {
        return BoletaDTO.builder()
                .idBoleta(b.getIdBoleta())
                .idPedido(b.getPedido() != null ? b.getPedido().getIdPedido() : null)
                .numeroBoleta(numeroDe(b.getIdBoleta()))
                .subtotal(b.getSubtotal())
                .igv(b.getIgv())
                .total(b.getTotal())
                .build();
    }
}
