package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovReporteDTO {
    private String tipo;
    private String operacion;
    private long registros;
}
