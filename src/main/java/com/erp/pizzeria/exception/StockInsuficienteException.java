package com.erp.pizzeria.exception;

import com.erp.pizzeria.dto.StockFaltanteDTO;

import java.util.Collections;
import java.util.List;

public class StockInsuficienteException extends RuntimeException {

    private final transient List<StockFaltanteDTO> faltantes;

    public StockInsuficienteException(String mensaje, List<StockFaltanteDTO> faltantes) {
        super(mensaje);
        this.faltantes = faltantes != null ? faltantes : Collections.emptyList();
    }

    public List<StockFaltanteDTO> getFaltantes() {
        return faltantes;
    }
}
