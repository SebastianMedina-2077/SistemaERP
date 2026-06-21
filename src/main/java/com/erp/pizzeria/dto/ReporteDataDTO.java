package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Conjunto completo de datos del panel de reportes, reutilizado por la vista y la exportacion (PDF/Excel). */
@Getter
@AllArgsConstructor
public class ReporteDataDTO {
    private String generadoEn;
    private List<StatDTO> salesStats;
    private List<TopProductoDTO> topProductos;
    private List<TipoMovReporteDTO> movimientosPorTipo;
    private List<ProveedorReporteDTO> comprasPorProveedor;
    private List<AnuladoReporteDTO> anulados;
}
