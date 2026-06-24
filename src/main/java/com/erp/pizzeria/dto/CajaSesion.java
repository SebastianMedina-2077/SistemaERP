package com.erp.pizzeria.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Estado en memoria de la caja de un cajero durante su turno.
 * No se persiste: vive mientras corre el servidor.
 */
@Getter
@Setter
public class CajaSesion {
    private Integer cajeroId;
    private String cajero;
    private BigDecimal montoInicial;
    private LocalDateTime fechaApertura;
    private boolean bloqueada;
    private int intentos;
    private BigDecimal ultimaDiferencia;
}
