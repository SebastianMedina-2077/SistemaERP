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
    private String tipoComprobante;
    private String mesa;
    private String emailEstado;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;

    public static BoletaDTO from(Boleta b) {
        return BoletaDTO.builder()
                .idBoleta(b.getIdBoleta())
                .idPedido(b.getPedido() != null ? b.getPedido().getIdPedido() : null)
                .numeroBoleta(b.getNumeroFormateado())
                .tipoComprobante(b.getTipoComprobante() != null ? b.getTipoComprobante().name() : null)
                .mesa(b.getMesa())
                .emailEstado(b.getEmailEstado())
                .subtotal(b.getSubtotal())
                .igv(b.getIgv())
                .total(b.getTotal())
                .build();
    }
}
