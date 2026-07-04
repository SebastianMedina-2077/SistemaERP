package com.erp.pizzeria.controller;

import com.erp.pizzeria.model.ClienteEmpresa;
import com.erp.pizzeria.service.ClienteEmpresaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Consulta y registro de RUC de clientes-empresa para la facturacion.
 * Bajo /cajero/** (rol CAJERO en SecurityConfig).
 */
@RestController
@RequestMapping("/cajero/clientes-empresa")
public class ClienteEmpresaRestController {

    private final ClienteEmpresaService service;

    public ClienteEmpresaRestController(ClienteEmpresaService service) {
        this.service = service;
    }

    /** Busca la razon social por RUC. Devuelve {encontrado:false} si no esta registrado. */
    @GetMapping("/{ruc}")
    public Map<String, Object> buscar(@PathVariable String ruc) {
        Optional<ClienteEmpresa> empresa = service.buscarPorRuc(ruc);
        return empresa
                .<Map<String, Object>>map(e -> Map.of(
                        "encontrado", true,
                        "ruc", e.getRuc(),
                        "razonSocial", e.getRazonSocial()))
                .orElse(Map.of("encontrado", false));
    }

    /** Registra un RUC con su razon social. */
    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, String> body) {
        try {
            ClienteEmpresa empresa = service.registrar(body.get("ruc"), body.get("razonSocial"));
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "ruc", empresa.getRuc(),
                    "razonSocial", empresa.getRazonSocial()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "mensaje", ex.getMessage()));
        }
    }
}
