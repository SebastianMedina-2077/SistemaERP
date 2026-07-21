package com.erp.pizzeria.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de tarjeta de un pago web. O bien referencia una tarjeta guardada
 * (idGuardada) o trae una nueva. El numero completo SOLO se usa en memoria para
 * derivar marca y ultimos 4: jamas se persiste, se devuelve ni se registra en logs.
 * El CVV ni siquiera viaja al backend (se valida solo en pantalla).
 */
@Getter
@Setter
@NoArgsConstructor
public class TiendaTarjetaDTO {

    /** Id de una tarjeta ya guardada de la cuenta. Excluye a los demas campos. */
    private Integer idGuardada;

    @Size(max = 19, message = "El numero de tarjeta no es valido")
    private String numero;

    @Size(max = 80, message = "El titular admite hasta 80 caracteres")
    private String titular;

    /** Vigencia MM/AA. Solo se valida; no se guarda. */
    @Size(max = 5, message = "Usa el formato MM/AA")
    private String vencimiento;

    /** Si es true y la tarjeta es nueva, se guardan marca + ultimos 4 + titular. */
    private Boolean guardar;
}
