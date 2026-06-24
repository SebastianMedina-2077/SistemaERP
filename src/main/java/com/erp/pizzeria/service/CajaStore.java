package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.CajaSesion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacen en memoria de las cajas abiertas por cajero. Sin base de datos.
 */
@Component
public class CajaStore {

    private static final int MAX_INTENTOS = 3;

    private final Map<Integer, CajaSesion> sesiones = new ConcurrentHashMap<>();
    private final String supervisorPin;

    public CajaStore(@Value("${caja.supervisor-pin:1234}") String supervisorPin) {
        this.supervisorPin = supervisorPin;
    }

    public CajaSesion abrir(Integer cajeroId, String cajero, BigDecimal montoInicial) {
        CajaSesion s = new CajaSesion();
        s.setCajeroId(cajeroId);
        s.setCajero(cajero);
        s.setMontoInicial(montoInicial);
        s.setFechaApertura(LocalDateTime.now());
        s.setBloqueada(false);
        s.setIntentos(0);
        sesiones.put(cajeroId, s);
        return s;
    }

    public CajaSesion obtener(Integer cajeroId) {
        return sesiones.get(cajeroId);
    }

    public int registrarFalla(Integer cajeroId, BigDecimal diferencia) {
        CajaSesion s = sesiones.get(cajeroId);
        if (s == null) return 0;
        s.setIntentos(s.getIntentos() + 1);
        s.setUltimaDiferencia(diferencia);
        if (s.getIntentos() >= MAX_INTENTOS) s.setBloqueada(true);
        return s.getIntentos();
    }

    public void cerrar(Integer cajeroId) {
        sesiones.remove(cajeroId);
    }

    public boolean desbloquear(Integer cajeroId, String pin) {
        CajaSesion s = sesiones.get(cajeroId);
        if (s == null || !supervisorPin.equals(pin)) return false;
        s.setBloqueada(false);
        s.setIntentos(0);
        return true;
    }

    public boolean desbloquearAdmin(Integer cajeroId) {
        CajaSesion s = sesiones.get(cajeroId);
        if (s == null) return false;
        s.setBloqueada(false);
        s.setIntentos(0);
        return true;
    }

    public List<CajaSesion> bloqueadas() {
        return sesiones.values().stream().filter(CajaSesion::isBloqueada).toList();
    }

    public int getMaxIntentos() {
        return MAX_INTENTOS;
    }

    public String getSupervisorPin() {
        return supervisorPin;
    }
}
