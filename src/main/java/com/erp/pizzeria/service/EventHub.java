package com.erp.pizzeria.service;

import com.erp.pizzeria.event.PedidoEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canal SSE en memoria: cocina, cajero y admin se suscriben a /api/eventos y el
 * servidor les empuja los cambios de pedido en tiempo real.
 *
 * Nota: registro en memoria de una sola instancia. Si se corre en varias instancias
 * balanceadas, habria que mover los emitters a un broker (Redis pub/sub).
 */
@Service
public class EventHub {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        // 30 min de vida; el EventSource del navegador reconecta solo al expirar o cortarse.
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("conectado").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    /**
     * Reenvia el evento a las pantallas solo despues de confirmarse la transaccion,
     * para que cuando una pantalla re-consulte ya vea los datos persistidos.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPedido(PedidoEvent evento) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(evento.tipo()).data(evento.payload()));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
