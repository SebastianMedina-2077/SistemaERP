package com.erp.pizzeria.model.enums;

/**
 * Tipo de comprobante emitido por el POS. Cada tipo define su serie y su codigo
 * de tipo de comprobante SUNAT (usado en la cadena del QR):
 *  - BOLETA / BOLETA_ELECTRONICA: serie B001, tipo SUNAT 03.
 *  - FACTURA: serie F001, tipo SUNAT 01.
 * BOLETA y BOLETA_ELECTRONICA comparten el correlativo de la serie B001 (ambas son boletas);
 * la electronica ademas se envia por email.
 */
public enum TipoComprobante {

    BOLETA("B001", "03", "BOLETA DE VENTA ELECTRONICA"),
    BOLETA_ELECTRONICA("B001", "03", "BOLETA DE VENTA ELECTRONICA"),
    FACTURA("F001", "01", "FACTURA ELECTRONICA");

    private final String serie;
    private final String codigoSunat;
    private final String titulo;

    TipoComprobante(String serie, String codigoSunat, String titulo) {
        this.serie = serie;
        this.codigoSunat = codigoSunat;
        this.titulo = titulo;
    }

    public String getSerie() {
        return serie;
    }

    public String getCodigoSunat() {
        return codigoSunat;
    }

    public String getTitulo() {
        return titulo;
    }

    public boolean requiereEnvioEmail() {
        return this == BOLETA_ELECTRONICA;
    }
}
