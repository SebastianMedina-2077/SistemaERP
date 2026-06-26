package com.erp.pizzeria.controller;

import com.erp.pizzeria.service.EventHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Stream SSE de eventos de pedido. Lo consumen las tres pantallas (cocina, cajero,
 * admin) para refrescarse en tiempo real sin polling.
 */
@RestController
public class RealtimeController {

    private final EventHub eventHub;

    public RealtimeController(EventHub eventHub) {
        this.eventHub = eventHub;
    }

    @GetMapping(value = "/api/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos() {
        return eventHub.subscribe();
    }
}
