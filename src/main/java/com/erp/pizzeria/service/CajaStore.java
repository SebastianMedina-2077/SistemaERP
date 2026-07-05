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
        CajaSesion sesion = new CajaSesion();
        sesion.setCajeroId(cajeroId);
        sesion.setCajero(cajero);
        sesion.setMontoInicial(montoInicial);
        sesion.setFechaApertura(LocalDateTime.now());
        sesion.setBloqueada(false);
        sesion.setIntentos(0);
        sesiones.put(cajeroId, sesion);
        return sesion;
    }

    public CajaSesion obtener(Integer cajeroId) {
        return sesiones.get(cajeroId);
    }

    public int registrarFalla(Integer cajeroId, BigDecimal diferencia) {
        CajaSesion sesion = sesiones.get(cajeroId);
        if (sesion == null) return 0;
        sesion.setIntentos(sesion.getIntentos() + 1);
        sesion.setUltimaDiferencia(diferencia);
        if (sesion.getIntentos() >= MAX_INTENTOS) sesion.setBloqueada(true);
        return sesion.getIntentos();
    }

    public void cerrar(Integer cajeroId) {
        sesiones.remove(cajeroId);
    }

    public boolean desbloquear(Integer cajeroId, String pin) {
        CajaSesion sesion = sesiones.get(cajeroId);
        if (sesion == null || !supervisorPin.equals(pin)) return false;
        sesion.setBloqueada(false);
        sesion.setIntentos(0);
        return true;
    }

    public boolean desbloquearAdmin(Integer cajeroId) {
        CajaSesion sesion = sesiones.get(cajeroId);
        if (sesion == null) return false;
        sesion.setBloqueada(false);
        sesion.setIntentos(0);
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
