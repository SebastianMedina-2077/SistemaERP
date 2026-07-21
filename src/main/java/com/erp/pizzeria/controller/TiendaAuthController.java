package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.CuentaTiendaDTO;
import com.erp.pizzeria.dto.LoginTiendaDTO;
import com.erp.pizzeria.dto.RegistroTiendaDTO;
import com.erp.pizzeria.dto.SesionTiendaDTO;
import com.erp.pizzeria.service.TiendaCuentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Autenticacion de clientes de la tienda web (JWT, cadena stateless de /api/tienda/**).
 */
@RestController
@RequestMapping("/api/tienda")
public class TiendaAuthController {

    private final TiendaCuentaService cuentaService;

    public TiendaAuthController(TiendaCuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping("/auth/registro")
    public ResponseEntity<SesionTiendaDTO> registro(@Valid @RequestBody RegistroTiendaDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.registrar(datos));
    }

    @PostMapping("/auth/login")
    public SesionTiendaDTO login(@Valid @RequestBody LoginTiendaDTO datos) {
        return cuentaService.login(datos);
    }

    @GetMapping("/cuenta")
    public CuentaTiendaDTO cuenta() {
        return cuentaService.perfilActual();
    }

    @ExceptionHandler(TiendaCuentaService.EmailYaRegistradoException.class)
    public ResponseEntity<Map<String, String>> emailDuplicado(TiendaCuentaService.EmailYaRegistradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> credencialesInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Credenciales incorrectas"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>> cuentaInactiva(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "La cuenta esta inactiva"));
    }
}
