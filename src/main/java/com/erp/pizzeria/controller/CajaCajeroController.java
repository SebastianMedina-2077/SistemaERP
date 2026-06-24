package com.erp.pizzeria.controller;

import com.erp.pizzeria.dto.CajaReporteDTO;
import com.erp.pizzeria.dto.CajaSesion;
import com.erp.pizzeria.exception.ResourceNotFoundException;
import com.erp.pizzeria.model.Usuario;
import com.erp.pizzeria.model.enums.EstadoPedido;
import com.erp.pizzeria.repository.PagoRepository;
import com.erp.pizzeria.repository.UsuarioRepository;
import com.erp.pizzeria.service.CajaStore;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/cajero/caja")
public class CajaCajeroController {

    private static final BigDecimal MONTO_DEFECTO = new BigDecimal("400.00");

    private final CajaStore cajaStore;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;

    public CajaCajeroController(CajaStore cajaStore,
                               UsuarioRepository usuarioRepository,
                               PagoRepository pagoRepository) {
        this.cajaStore = cajaStore;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
    }

    @GetMapping("/estado")
    public Map<String, Object> estado(Authentication auth) {
        Usuario u = usuario(auth);
        CajaSesion s = cajaStore.obtener(u.getIdUsuario());
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("abierta", s != null);
        r.put("bloqueada", s != null && s.isBloqueada());
        r.put("intentos", s != null ? s.getIntentos() : 0);
        r.put("maxIntentos", cajaStore.getMaxIntentos());
        r.put("montoInicial", s != null ? s.getMontoInicial() : MONTO_DEFECTO);
        return r;
    }

    @PostMapping("/abrir")
    public Map<String, Object> abrir(@RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        Usuario u = usuario(auth);
        // Monto opcional: si llega vacio o invalido, se usa el monto por defecto.
        BigDecimal monto = parseMonto(body == null ? null : body.get("montoInicial"));
        if (monto == null || monto.signum() < 0) monto = MONTO_DEFECTO;
        cajaStore.abrir(u.getIdUsuario(), auth.getName(), monto);
        return Map.of("abierta", true, "montoInicial", monto);
    }

    @GetMapping("/reporte")
    public ResponseEntity<?> reporte(Authentication auth) {
        Usuario u = usuario(auth);
        CajaSesion s = cajaStore.obtener(u.getIdUsuario());
        if (s == null) return sinCaja();
        return ResponseEntity.ok(construirReporte(u.getIdUsuario(), s));
    }

    @PostMapping("/cuadre")
    public ResponseEntity<?> cuadre(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario u = usuario(auth);
        CajaSesion s = cajaStore.obtener(u.getIdUsuario());
        if (s == null) return sinCaja();
        // El monto contado es obligatorio: rechaza valores vacios, no numericos o negativos.
        BigDecimal contado = parseMonto(body == null ? null : body.get("contado"));
        if (contado == null || contado.signum() < 0) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Monto contado invalido."));
        }
        boolean confirmar = body.get("confirmar") != null
                && Boolean.parseBoolean(body.get("confirmar").toString());
        CajaReporteDTO rep = construirReporte(u.getIdUsuario(), s);
        BigDecimal diferencia = contado.subtract(rep.getEfectivoEsperado());
        int signo = diferencia.compareTo(BigDecimal.ZERO);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("esperado", rep.getEfectivoEsperado());
        r.put("contado", contado);
        r.put("diferencia", diferencia);

        if (signo < 0) {
            // Falta efectivo: cuenta como intento fallido; a los 3 la caja se bloquea.
            int intentos = cajaStore.registrarFalla(u.getIdUsuario(), diferencia);
            boolean bloqueada = cajaStore.obtener(u.getIdUsuario()).isBloqueada();
            r.put("resultado", bloqueada ? "BLOQUEADA" : "FALTA");
            r.put("intentos", intentos);
            r.put("intentosRestantes", Math.max(0, cajaStore.getMaxIntentos() - intentos));
            r.put("bloqueada", bloqueada);
        } else if (signo > 0) {
            // Sobrante: exige una segunda confirmacion antes de cerrar el turno.
            r.put("resultado", "SOBRA");
            if (confirmar) {
                cajaStore.cerrar(u.getIdUsuario());
                r.put("cerrada", true);
            } else {
                r.put("requiereConfirmacion", true);
                r.put("cerrada", false);
            }
        } else {
            // Cuadre exacto: cierra el turno directamente.
            r.put("resultado", "CUADRA");
            cajaStore.cerrar(u.getIdUsuario());
            r.put("cerrada", true);
        }
        return ResponseEntity.ok(r);
    }

    @PostMapping("/desbloquear")
    public ResponseEntity<Map<String, Object>> desbloquear(@RequestBody Map<String, Object> body, Authentication auth) {
        Usuario u = usuario(auth);
        String pin = body.get("pin") == null ? "" : body.get("pin").toString();
        return cajaStore.desbloquear(u.getIdUsuario(), pin)
                ? ResponseEntity.ok(Map.of("ok", true))
                : ResponseEntity.status(403).body(Map.of("ok", false, "mensaje", "Codigo incorrecto"));
    }

    private CajaReporteDTO construirReporte(Integer cajeroId, CajaSesion s) {
        Map<String, BigDecimal> porMetodo = new LinkedHashMap<>();
        for (Object[] fila : pagoRepository.resumenPorMetodo(cajeroId, s.getFechaApertura(), EstadoPedido.ANULADO)) {
            porMetodo.put((String) fila[0], (BigDecimal) fila[1]);
        }
        BigDecimal efectivo = porMetodo.getOrDefault("Efectivo", BigDecimal.ZERO);
        BigDecimal tarjeta = porMetodo.getOrDefault("Tarjeta", BigDecimal.ZERO);
        BigDecimal yape = porMetodo.getOrDefault("Yape", BigDecimal.ZERO);
        BigDecimal plin = porMetodo.getOrDefault("Plin", BigDecimal.ZERO);
        BigDecimal totalVentas = efectivo.add(tarjeta).add(yape).add(plin);
        BigDecimal esperado = s.getMontoInicial().add(efectivo);
        return new CajaReporteDTO(s.getMontoInicial(), efectivo, tarjeta, yape, plin, totalVentas, esperado);
    }

    private ResponseEntity<Map<String, Object>> sinCaja() {
        return ResponseEntity.status(409).body(Map.of(
                "sinCaja", true,
                "montoInicial", MONTO_DEFECTO,
                "mensaje", "No hay una caja abierta. Abre la caja para continuar."));
    }

    private Usuario usuario(Authentication auth) {
        return usuarioRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + auth.getName()));
    }

    /** Convierte un valor del JSON a BigDecimal; devuelve null si es nulo, vacio o no numerico. */
    private static BigDecimal parseMonto(Object valor) {
        if (valor == null) return null;
        String s = valor.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
