// Respaldo lento: el refresco real llega por SSE; este intervalo solo cubre una
// posible caida del stream.
const REFRESH_MS = 30000;
const SALIDA_MS = 5000; // tiempo visible de la card tras marcarse atendida

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

const kitchenOrders = document.querySelector("#kitchenOrders");
const refreshBtn = document.querySelector("#cocinaRefresh");
const colaCount = document.querySelector("#colaCount");

let pedidos = [];
let conocidos = new Set();
let primeraCarga = true;
// Pedidos con animacion de salida en curso: mientras existan, no se re-renderiza la cola.
let animando = new Set();

function formatId(id) {
  return `PED-${String(id).padStart(3, "0")}`;
}

function minutosEspera(iso) {
  const t = iso ? new Date(iso).getTime() : NaN;
  if (Number.isNaN(t)) return 0;
  return Math.max(0, Math.floor((Date.now() - t) / 60000));
}

// Semaforo por tiempo en cola: verde < 6 min, naranja 6-10 min, rojo > 10 min.
function claseUrgencia(min) {
  if (min > 10) return "estado-urgente";
  if (min >= 6) return "estado-demora";
  return "estado-reciente";
}

function textoEspera(min) {
  return min <= 0 ? "recien" : `hace ${min} min`;
}

async function loadKitchen() {
  try {
    const res = await fetch("/api/pedidos/cocina", { headers: { Accept: "application/json" } });
    if (!res.ok) throw new Error(res.statusText);
    pedidos = await res.json();
    detectarNuevos(pedidos);
    render();
  } catch (err) {
    if (animando.size) return; // no pisar una animacion de salida en curso
    kitchenOrders.className = "cocina-pedidos-grid empty-state";
    kitchenOrders.textContent = `No se pudo cargar la cola: ${err.message}`;
  }
}

function detectarNuevos(lista) {
  if (!primeraCarga) {
    lista.forEach((p) => {
      if (!conocidos.has(p.idPedido)) {
        MammaTomatoAlert.info("Nuevo pedido recibido", `${formatId(p.idPedido)} · ${p.cliente ?? "Cliente"}`, 6000);
      }
    });
  }
  conocidos = new Set(lista.map((p) => p.idPedido));
  primeraCarga = false;
}

function actualizarColaCount() {
  if (colaCount) colaCount.textContent = pedidos.length;
}

function render() {
  actualizarColaCount();
  // Mientras una card sale (atendido), no destruir la cola para no cortar la animacion.
  if (animando.size) return;

  // Cola: el pedido mas demorado primero (rojo), respetando el orden de llegada.
  const visibles = [...pedidos].sort((a, b) => new Date(a.fecha) - new Date(b.fecha));

  if (!visibles.length) {
    kitchenOrders.className = "cocina-pedidos-grid empty-state";
    kitchenOrders.replaceChildren(estadoVacio());
    return;
  }
  kitchenOrders.className = "cocina-pedidos-grid";
  kitchenOrders.replaceChildren(...visibles.map(renderCard));
}

function estadoVacio() {
  const box = document.createElement("div");
  box.className = "cocina-vacio";
  box.innerHTML = `<i class="bi bi-check2-all"></i><p>Sin pedidos en cola</p><span>Los nuevos pedidos apareceran aqui</span>`;
  return box;
}

function renderCard(pedido) {
  const estado = String(pedido.estado).toUpperCase();
  const min = minutosEspera(pedido.fecha);
  const card = document.createElement("article");
  card.className = `cocina-pedido-card ${claseUrgencia(min)}`;
  card.dataset.id = pedido.idPedido;

  const header = document.createElement("div");
  header.className = "cocina-card-header";
  const id = document.createElement("span");
  id.className = "cocina-card-id";
  id.textContent = formatId(pedido.idPedido);
  const tiempo = document.createElement("span");
  tiempo.className = "cocina-card-tiempo";
  tiempo.textContent = textoEspera(min);
  header.append(id, tiempo);

  const cliente = document.createElement("div");
  cliente.className = "cocina-card-cliente";
  const nombre = document.createElement("strong");
  nombre.textContent = pedido.cliente ?? "Sin nombre";
  const etiqueta = document.createElement("span");
  etiqueta.textContent = estado === "PENDIENTE" ? "Pendiente" : "Preparando";
  cliente.append(nombre, etiqueta);

  const items = document.createElement("div");
  items.className = "cocina-card-items";
  (pedido.items || []).forEach((it) => {
    const row = document.createElement("div");
    row.className = "cocina-item";
    const qty = document.createElement("span");
    qty.className = "cocina-item-qty";
    qty.textContent = `${it.cantidad}x`;
    const texto = document.createElement("div");
    const prod = document.createElement("span");
    prod.className = "cocina-item-nombre";
    prod.textContent = it.producto;
    texto.append(prod);
    if (it.observacion) {
      const obs = document.createElement("span");
      obs.className = "cocina-item-obs";
      obs.textContent = it.observacion;
      texto.append(obs);
    }
    row.append(qty, texto);
    items.append(row);
  });

  const footer = document.createElement("div");
  footer.className = "cocina-card-footer";
  if (estado === "PENDIENTE") {
    footer.append(accionBtn("Empezar a preparar", "accion-preparar", pedido.idPedido, "PREPARANDO"));
  } else {
    footer.append(accionBtn("Marcar entregado", "accion-entregar", pedido.idPedido, "ATENDIDO"));
  }

  card.append(header, cliente, items, footer);
  return card;
}

function accionBtn(text, clase, idPedido, estado) {
  const b = document.createElement("button");
  b.type = "button";
  b.className = `btn-cocina-accion ${clase}`;
  b.textContent = text;
  b.addEventListener("click", () => updateStatus(idPedido, estado, b));
  return b;
}

async function updateStatus(idPedido, estado, btn) {
  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
  btn.disabled = true;
  try {
    const res = await fetch(`/api/pedidos/${idPedido}/estado`, {
      method: "PATCH",
      headers,
      body: JSON.stringify({ estado }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.mensaje || res.statusText);
    }
    if (estado === "ATENDIDO") {
      MammaTomatoAlert.success("Pedido entregado", `${formatId(idPedido)} marcado como entregado`);
      animarAtendido(idPedido, btn.closest(".cocina-pedido-card"));
    } else {
      MammaTomatoAlert.info("Estado actualizado", `${formatId(idPedido)} en preparacion`);
      await loadKitchen();
    }
  } catch (err) {
    MammaTomatoAlert.error("No se pudo actualizar el estado", err.message);
    btn.disabled = false;
  }
}

// La card atendida queda 5s con barra de progreso y luego sale con animacion.
function animarAtendido(idPedido, card) {
  if (!card) { loadKitchen(); return; }
  animando.add(idPedido);
  card.classList.add("atendido");
  const footer = card.querySelector(".cocina-card-footer");
  if (footer) {
    footer.innerHTML =
      '<div class="cocina-atendido-box">' +
      '  <div class="cocina-atendido-label"><i class="bi bi-check-circle-fill"></i> Atendido</div>' +
      '  <div class="cocina-progress"><div class="cocina-progress-bar"></div></div>' +
      '</div>';
  }
  setTimeout(() => {
    card.classList.add("saliendo");
    setTimeout(() => {
      animando.delete(idPedido);
      conocidos.delete(idPedido);
      pedidos = pedidos.filter((p) => p.idPedido !== idPedido);
      card.remove();
      render();
    }, 450);
  }, SALIDA_MS);
}

if (refreshBtn) refreshBtn.addEventListener("click", loadKitchen);

// Tiempo real: refresca al instante cuando entra un pedido nuevo o cambia un estado.
if (window.MammaTomatoRealtime) {
  window.MammaTomatoRealtime.on("pedido-nuevo", loadKitchen);
  window.MammaTomatoRealtime.on("pedido-estado", loadKitchen);
}

loadKitchen();
setInterval(loadKitchen, REFRESH_MS);