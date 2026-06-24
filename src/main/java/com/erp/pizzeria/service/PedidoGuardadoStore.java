package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.PedidoGuardadoDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Almacen en memoria de pedidos "en espera" por cajero.
 * No usa base de datos: la vida util es la del servidor (pedidos temporales).
 */
@Component
public class PedidoGuardadoStore {

    private final Map<Integer, List<PedidoGuardadoDTO>> porCajero = new ConcurrentHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(0);

    public PedidoGuardadoDTO guardar(Integer cajeroId, PedidoGuardadoDTO dto) {
        String cliente = (dto.getCliente() == null || dto.getCliente().isBlank())
                ? "Sin nombre" : dto.getCliente().trim();
        dto.setId(secuencia.incrementAndGet());
        dto.setCliente(cliente);
        dto.setReferencia(cliente);
        dto.setFechaGuardado(LocalDateTime.now());
        lista(cajeroId).add(dto);
        return dto;
    }

    public List<PedidoGuardadoDTO> listar(Integer cajeroId) {
        return List.copyOf(lista(cajeroId));
    }

    public int contar(Integer cajeroId) {
        return lista(cajeroId).size();
    }

    public Optional<PedidoGuardadoDTO> recuperar(Integer cajeroId, Long id) {
        List<PedidoGuardadoDTO> lista = lista(cajeroId);
        Optional<PedidoGuardadoDTO> encontrado = lista.stream()
                .filter(p -> p.getId().equals(id)).findFirst();
        encontrado.ifPresent(lista::remove);
        return encontrado;
    }

    public boolean eliminar(Integer cajeroId, Long id) {
        return lista(cajeroId).removeIf(p -> p.getId().equals(id));
    }

    private List<PedidoGuardadoDTO> lista(Integer cajeroId) {
        return porCajero.computeIfAbsent(cajeroId, k -> new CopyOnWriteArrayList<>());
    }
}
