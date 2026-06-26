// Canal SSE compartido. Abre una sola conexion a /api/eventos y reparte los eventos
// de pedido a quien se suscriba. El EventSource del navegador reconecta solo si se corta.
(function () {
  "use strict";
  if (!window.EventSource) return;

  const handlers = {};

  function emitir(nombre, evento) {
    let data = {};
    try { data = JSON.parse(evento.data); } catch (_) { /* payload no-JSON: se ignora */ }
    (handlers[nombre] || []).forEach((fn) => {
      try { fn(data); } catch (_) { /* un handler que falla no tumba a los demas */ }
    });
  }

  function conectar() {
    const es = new EventSource("/api/eventos");
    es.addEventListener("pedido-nuevo", (e) => emitir("pedido-nuevo", e));
    es.addEventListener("pedido-estado", (e) => emitir("pedido-estado", e));
    // onerror: EventSource reintenta por su cuenta; no hace falta reconectar a mano.
  }

  window.MammaTomatoRealtime = {
    on(nombre, handler) {
      (handlers[nombre] = handlers[nombre] || []).push(handler);
    },
  };

  conectar();
})();
