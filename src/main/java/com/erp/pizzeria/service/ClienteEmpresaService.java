package com.erp.pizzeria.service;

import com.erp.pizzeria.model.ClienteEmpresa;
import com.erp.pizzeria.repository.ClienteEmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Registro de clientes con RUC (empresas) para facturacion.
 */
@Service
@Transactional(readOnly = true)
public class ClienteEmpresaService {

    private final ClienteEmpresaRepository repository;

    public ClienteEmpresaService(ClienteEmpresaRepository repository) {
        this.repository = repository;
    }

    public Optional<ClienteEmpresa> buscarPorRuc(String ruc) {
        return repository.findByRuc(ruc != null ? ruc.trim() : null);
    }

    /**
     * Registra o actualiza el RUC con su razon social. Idempotente: si ya existe,
     * actualiza la razon social.
     */
    @Transactional
    public ClienteEmpresa registrar(String ruc, String razonSocial) {
        String rucLimpio = ruc != null ? ruc.trim() : "";
        if (!rucLimpio.matches("\\d{11}")) {
            throw new IllegalArgumentException("El RUC debe tener 11 digitos numericos");
        }
        if (razonSocial == null || razonSocial.isBlank()) {
            throw new IllegalArgumentException("La razon social es obligatoria");
        }
        ClienteEmpresa empresa = repository.findByRuc(rucLimpio).orElseGet(ClienteEmpresa::new);
        empresa.setRuc(rucLimpio);
        empresa.setRazonSocial(razonSocial.trim());
        return repository.save(empresa);
    }
}
