package com.erp.pizzeria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {

    @NotNull
    @Size(max = 15)
    private String clienteNombre;

    @Size(max = 9)
    private String clienteTelefono;

    @NotNull
    private Integer idMetodoPago;

    @NotEmpty
    @Valid
    private List<DetallePedidoDTO> items;

    /** Desglose de pago. Opcional: si viene vacio se asume un solo pago con idMetodoPago. */
    @Valid
    private List<PagoDTO> pagos;

    // ---- Comprobante ----

    /** BOLETA (defecto), FACTURA o BOLETA_ELECTRONICA. */
    private String tipoComprobante;

    /** DNI del cliente (opcional, solo boleta). 8 digitos. */
    @Size(max = 8)
    private String clienteDni;

    /** RUC del cliente (solo factura). 11 digitos. */
    @Size(max = 11)
    private String clienteRuc;

    /** Razon social del cliente (solo factura). */
    @Size(max = 120)
    private String clienteRazonSocial;

    /** Email del cliente (solo boleta electronica). */
    @Size(max = 120)
    private String clienteEmail;

    /** Mesa o ubicacion: BARRA, MESA-1..MESA-4, o null (para llevar / sin mesa). */
    @Size(max = 20)
    private String mesa;
}
