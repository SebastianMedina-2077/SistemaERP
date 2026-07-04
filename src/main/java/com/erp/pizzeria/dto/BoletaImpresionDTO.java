package com.erp.pizzeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo de impresion de la boleta tipo ticket de supermercado: agrupa los datos
 * de la empresa, el comprobante, el detalle y el QR (data-URI) para la vista.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletaImpresionDTO {

    // ---- Empresa ----
    private String nombreComercial;
    private String razonSocial;
    private String ruc;
    private String direccion;
    private String telefono;

    // ---- Comprobante ----
    private String titulo;
    private boolean factura;             // muestra RUC + razon social del cliente
    private String numero;               // B001-000123 / F001-000123
    private LocalDateTime fecha;
    private String cajero;
    private String cliente;
    private String clienteDocumento;     // DNI (boleta) o RUC (factura)
    private String clienteRazonSocial;   // solo factura
    private String mesa;

    // ---- Importes ----
    private BigDecimal subtotal;  // base imponible (sin IGV)
    private BigDecimal igv;
    private BigDecimal total;

    // ---- Detalle y pagos ----
    private List<Linea> items;
    private List<Pago> pagos;

    // ---- QR ----
    private String qrContenido;   // cadena SUNAT separada por "|"
    private String qrDataUri;     // data:image/png;base64,...

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Linea {
        private String nombre;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal importe;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Pago {
        private String metodo;
        private BigDecimal monto;
    }
}
