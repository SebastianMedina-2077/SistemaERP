package com.erp.pizzeria.service;

import com.erp.pizzeria.model.Auditoria;
import com.erp.pizzeria.repository.AuditoriaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Auditoria> listar() {
        return auditoriaRepository.findTop500ByOrderByFechaDescIdAuditoriaDesc();
    }

    /**
     * Persiste una entrada de auditoria en su propia transaccion para que el
     * registro nunca interfiera con la operacion de negocio que lo origino.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String accion, String entidad, String referencia) {
        Auditoria registro = new Auditoria();
        registro.setFecha(LocalDateTime.now());
        registro.setUsuario(usuarioActual());
        registro.setAccion(accion);
        registro.setEntidad(entidad);
        registro.setReferencia(referencia);
        auditoriaRepository.save(registro);
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "sistema";
        }
        return auth.getName();
    }
}
