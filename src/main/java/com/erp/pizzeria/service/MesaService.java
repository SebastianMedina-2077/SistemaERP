package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.MesaDTO;
import com.erp.pizzeria.event.PedidoEvent;
import com.erp.pizzeria.model.Mesa;
import com.erp.pizzeria.model.enums.EstadoMesa;
import com.erp.pizzeria.repository.MesaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Ciclo de vida de las mesas numeradas del salon:
 * LIBRE -> OCUPADA (al crear un pedido con esa mesa) -> POR_LIMPIAR
 * (el cliente se retira) -> LIBRE (el cajero la marca limpia).
 *
 * Cada transicion que cambia el estado empuja un evento SSE "mesa" con
 * {numero, estado} para que las pantallas de caja se refresquen en vivo.
 */
@Service
@Transactional
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MesaService(MesaRepository mesaRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.mesaRepository = mesaRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<MesaDTO> listar() {
        return mesaRepository.findAllByOrderByNumeroAsc().stream()
                .map(this::aDTO)
                .toList();
    }

    /**
     * Marca la mesa como OCUPADA. Idempotente: si ya estaba OCUPADA no falla ni
     * reemite el evento. Si la mesa no existe se ignora en silencio: el numero
     * viene de un texto libre ("MESA-N") y una mesa fuera de rango no es un error.
     */
    public void ocupar(int numero) {
        Mesa mesa = mesaRepository.findByNumero(numero).orElse(null);
        if (mesa == null || mesa.getEstado() == EstadoMesa.OCUPADA) {
            return;
        }
        mesa.setEstado(EstadoMesa.OCUPADA);
        mesaRepository.save(mesa);
        publicar(mesa);
    }

    /** OCUPADA -> POR_LIMPIAR: el cajero la manda a limpiar cuando el cliente se retira. */
    public MesaDTO mandarALimpiar(int numero) {
        Mesa mesa = obtener(numero);
        if (mesa.getEstado() != EstadoMesa.OCUPADA) {
            throw new IllegalArgumentException("La mesa " + numero + " no esta ocupada");
        }
        mesa.setEstado(EstadoMesa.POR_LIMPIAR);
        mesaRepository.save(mesa);
        publicar(mesa);
        return aDTO(mesa);
    }

    /** POR_LIMPIAR -> LIBRE: la mesa queda limpia y disponible de nuevo. */
    public MesaDTO marcarLimpia(int numero) {
        Mesa mesa = obtener(numero);
        if (mesa.getEstado() != EstadoMesa.POR_LIMPIAR) {
            throw new IllegalArgumentException("La mesa " + numero + " no esta por limpiar");
        }
        mesa.setEstado(EstadoMesa.LIBRE);
        mesaRepository.save(mesa);
        publicar(mesa);
        return aDTO(mesa);
    }

    private Mesa obtener(int numero) {
        return mesaRepository.findByNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("La mesa " + numero + " no existe"));
    }

    private void publicar(Mesa mesa) {
        eventPublisher.publishEvent(new PedidoEvent("mesa",
                Map.of("numero", mesa.getNumero(), "estado", mesa.getEstado().name())));
    }

    private MesaDTO aDTO(Mesa mesa) {
        return new MesaDTO(mesa.getNumero(), mesa.getCapacidad(), mesa.getEstado().name());
    }
}
