package com.erp.pizzeria.controller;

import com.erp.pizzeria.model.enums.TipoComprobante;
import com.erp.pizzeria.service.CorrelativoService;
import com.erp.pizzeria.service.GeneradorQrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Apoyo al modal de orden del POS: numero de comprobante previsto y QR de pago
 * para billeteras digitales (Yape/Plin). Bajo /cajero/** (rol CAJERO).
 */
@RestController
public class PosComprobanteController {

    private final CorrelativoService correlativoService;
    private final GeneradorQrService generadorQrService;

    public PosComprobanteController(CorrelativoService correlativoService,
                                    GeneradorQrService generadorQrService) {
        this.correlativoService = correlativoService;
        this.generadorQrService = generadorQrService;
    }

    /**
     * Numero PREVISTO del proximo comprobante de la serie (estimacion para mostrar
     * en el modal; el numero definitivo se asigna al completar la orden).
     */
    @GetMapping("/cajero/comprobante/siguiente")
    public Map<String, Object> siguiente(@RequestParam(defaultValue = "BOLETA") String tipo) {
        TipoComprobante t = parseTipo(tipo);
        int numero = correlativoService.proximoPrevisto(t.getSerie());
        return Map.of(
                "serie", t.getSerie(),
                "correlativo", numero,
                "numero", String.format("%s-%06d", t.getSerie(), numero));
    }

    /**
     * QR de pago FICTICIO (placeholder) para billeteras digitales. Codifica una URL
     * de pago de ejemplo con el metodo y el monto. No corresponde a una pasarela real.
     */
    @GetMapping("/cajero/pago/qr")
    public ResponseEntity<byte[]> qrPago(@RequestParam(defaultValue = "Yape") String metodo,
                                         @RequestParam(defaultValue = "0.00") String monto) {
        String contenido = String.format(Locale.ROOT,
                "https://pago.mammatomato.test/%s?monto=%s", metodo.toLowerCase(Locale.ROOT), monto);
        byte[] png = generadorQrService.generarPng(contenido, 240);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    private TipoComprobante parseTipo(String tipo) {
        try {
            return TipoComprobante.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return TipoComprobante.BOLETA;
        }
    }
}
