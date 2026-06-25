package com.erp.pizzeria.dto;

import com.erp.pizzeria.model.DetalleMovimiento;
import com.erp.pizzeria.model.Movimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoComprobanteDTO {
    private Integer idMovimiento;
    private LocalDate fecha;
    private String tipo;
    private String operacion;
    private String documento;
    private String glosa;
    private String usuario;
    private List<Linea> lineas;

    public static MovimientoComprobanteDTO from(Movimiento movimiento, List<DetalleMovimiento> detalles) {
        return MovimientoComprobanteDTO.builder()
                .idMovimiento(movimiento.getIdMovimiento())
                .fecha(movimiento.getFecha())
                .tipo(movimiento.getTipoMovimiento().getDescripcion())
                .operacion(movimiento.getTipoMovimiento().getOperacion())
                .documento(movimiento.getDocumento())
                .glosa(movimiento.getGlosa())
                .usuario(nombreUsuario(movimiento))
                .lineas(detalles.stream().map(Linea::from).toList())
                .build();
    }

    private static String nombreUsuario(Movimiento movimiento) {
        if (movimiento.getUsuario().getEmpleado() != null) {
            return movimiento.getUsuario().getEmpleado().getNombre();
        }
        return movimiento.getUsuario().getUsername();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Linea {
        private String codigo;
        private String insumo;
        private BigDecimal cantidad;
        private BigDecimal stockResultante;
        private String medida;

        public static Linea from(DetalleMovimiento detalle) {
            return Linea.builder()
                    .codigo(detalle.getInsumo().getCodigo())
                    .insumo(detalle.getInsumo().getNombre())
                    .cantidad(detalle.getCantidad())
                    .stockResultante(detalle.getStockResultante())
                    .medida(detalle.getInsumo().getMedida().getSigla())
                    .build();
        }
    }
}
