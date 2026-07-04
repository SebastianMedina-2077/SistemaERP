package com.erp.pizzeria.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador del correlativo por serie de comprobante. Garantiza una numeracion
 * secuencial y SIN HUECOS: el numero se asigna incrementando esta fila dentro de
 * la misma transaccion de la venta, de modo que un rollback deshace el incremento
 * (a diferencia del AUTO_INCREMENT de MySQL, que no reutiliza ids descartados).
 */
@Entity
@Table(name = "comprobante_correlativo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteCorrelativo {

    @Id
    @Column(name = "serie", length = 4)
    private String serie;

    @Column(name = "ultimo_numero", nullable = false)
    private Integer ultimoNumero;
}
