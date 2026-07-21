package com.erp.pizzeria.controller;

import com.erp.pizzeria.service.GeneradorQrService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

/** Vista pública de la tienda en línea; el catálogo se carga por API. */
@Controller
public class TiendaController {

    private final GeneradorQrService generadorQr;

    public TiendaController(GeneradorQrService generadorQr) {
        this.generadorQr = generadorQr;
    }

    @GetMapping("/tienda")
    public String tienda() {
        return "tienda/tienda";
    }

    /** Ingreso/registro del cliente en página propia (no modal); el JWT lo emite /api/tienda/auth. */
    @GetMapping("/tienda/ingresar")
    public String ingresar() {
        return "tienda/ingresar";
    }

    /**
     * Página informativa de pago con billetera digital (Yape/Plin). Demostración académica:
     * no procesa cobros, el flujo real de pago sigue en el checkout de la tienda.
     */
    @GetMapping("/tienda/pago-billetera")
    public String pagoBilletera(@RequestParam(defaultValue = "yape") String metodo, Model model) {
        model.addAttribute("metodo", normalizarMetodo(metodo));
        return "tienda/billetera";
    }

    /**
     * QR demo para la página de billetera. Payload sin datos reales (no hay pasarela):
     * es solo un placeholder visual coherente con la marca.
     */
    @GetMapping(value = "/tienda/pago-billetera/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrBilletera(@RequestParam(defaultValue = "yape") String metodo) {
        String billetera = normalizarMetodo(metodo).toUpperCase(Locale.ROOT);
        byte[] png = generadorQr.generarPng("MAMMATOMATO|DEMO|" + billetera, 240);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache())
                .body(png);
    }

    /** Solo aceptamos yape/plin; cualquier otro valor cae en yape por defecto. */
    private String normalizarMetodo(String metodo) {
        if (metodo != null && "plin".equalsIgnoreCase(metodo.trim())) {
            return "plin";
        }
        return "yape";
    }
}
