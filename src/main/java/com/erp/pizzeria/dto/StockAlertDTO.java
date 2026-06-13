package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDTO {
    private boolean ok;
    private List<StockFaltanteDTO> faltantes;

    public static StockAlertDTO ok() {
        return new StockAlertDTO(true, List.of());
    }
}
