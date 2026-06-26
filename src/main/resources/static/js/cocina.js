const REFRESH_MS = 8000;
const SALIDA_MS = 5000; // tiempo visible de la card tras marcarse atendida

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

// Referencias Kanban de las 3 columnas
const colPendientes = document.querySelector("#colPendientes");
const colPreparando = document.querySelector("#colPreparando");
const colEntregados = document.querySelector("#colEntregados");

const countPendientes = document.querySelector("#countPendientes");
const countPreparando = document.querySelector("#countPreparando");
const countEntregados = document.querySelector("#countEntregados");

const refreshBtn = document.querySelector("#cocinaRefresh");
const colaCount = document.querySelector("#colaCount");

let pedidos = [];
let conocidos = new Set();
let primeraCarga = true;
// Pedidos con animacion de salida en curso: mientras existan, no se re-renderiza la cola por API.
let animando = new Set();

function formatId(id) {
  return `PED-${String(id).padStart(3, "0")}`;
}

function minutosEspera(iso) {
  const t = iso ? new Date(iso).getTime() : NaN;
  if (Number.isNaN(t)) return 0;
  return Math.max(0, Math.floor((Date.now() - t) / 60000));
}

function claseUrgencia(min) {
  if (min > 10) return "estado-urgente";
  if (min >= 6) return "estado-demora";
  return "estado-reciente";
}

function textoEspera(min) {
  return min <= 0 ? "recién" : `hace ${min} min`;
}

async function loadKitchen() {
  try {
    const res = await fetch("/api/pedidos/cocina", { headers: { Accept: "application/json" } });
    if (!res.ok) throw new Error(res.statusText);

    const datosNuevos = await res.json();
    detectarNuevos(datosNuevos);

    // Si hay elementos animándose en la salida, no machacamos los pedidos locales para no romper el DOM
    if (animando.size) return;

    pedidos = datosNuevos;
    render();
  } catch (err) {
    if (animando.size) return;
    if (colPendientes && colPreparando && colEntregados) {
      colPendientes.innerHTML = `<div class="cocina-error">Error al cargar</div>`;
      colPreparando.innerHTML = `<div class="cocina-error">${err.message}</div>`;
      colEntregados.innerHTML = ``;
    }
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

function actualizarContadores(pendientes, preparando, entregados) {
  if (colaCount) colaCount.textContent = pedidos.length;
  if (countPendientes) countPendientes.textContent = pendientes.length;
  if (countPreparando) countPreparando.textContent = preparando.length;
  if (countEntregados) countEntregados.textContent = entregados.length;
}

function render() {
  // Ordenar respetando el orden cronológico de llegada
  const pedidosOrdenados = [...pedidos].sort((a, b) => new Date(a.fecha) - new Date(b.fecha));

  const listaPendientes = pedidosOrdenados.filter(p => String(p.estado).toUpperCase() === "PENDIENTE");
  const listaPreparando = pedidosOrdenados.filter(p => String(p.estado).toUpperCase() === "PREPARANDO");
  const listaEntregados = pedidosOrdenados.filter(p => String(p.estado).toUpperCase() === "ATENDIDO");

  actualizarContadores(listaPendientes, listaPreparando, listaEntregados);

  // 1. Renderizar Pendientes
  if (!listaPendientes.length) {
    colPendientes.replaceChildren(estadoVacio("Sin pedidos pendientes"));
  } else {
    colPendientes.replaceChildren(...listaPendientes.map(renderCard));
  }

  // 2. Renderizar Preparando
  if (!listaPreparando.length) {
    colPreparando.replaceChildren(estadoVacio("Sin platos en preparación"));
  } else {
    colPreparando.replaceChildren(...listaPreparando.map(renderCard));
  }

  // 3. Renderizar Entregados (para las tarjetas que se están desvaneciendo)
  if (!listaEntregados.length) {
    colEntregados.replaceChildren(estadoVacio("Historial vacío"));
  } else {
    colEntregados.replaceChildren(...listaEntregados.map(renderCard));
  }
}

function estadoVacio(mensaje) {
  const box = document.createElement("div");
  box.className = "cocina-vacio";
  box.innerHTML = `<i class="bi bi-check2-all"></i><p>${mensaje}</p>`;
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
  etiqueta.className = `badge-estado-${estado.toLowerCase()}`;
  etiqueta.textContent = estado === "PENDIENTE" ? "Pendiente" : estado === "PREPARANDO" ? "Preparando" : "Entregado";
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
  } else if (estado === "PREPARANDO") {
    footer.append(accionBtn("Marcar entregado", "accion-entregar", pedido.idPedido, "ATENDIDO"));
  } else if (estado === "ATENDIDO") {
    // Si se renderiza directamente un estado ATENDIDO en proceso de salida
    card.classList.add("atendido");
    footer.innerHTML =
        '<div class="cocina-atendido-box">' +
        '  <div class="cocina-atendido-label"><i class="bi bi-check-circle-fill"></i> Atendido</div>' +
        '  <div class="cocina-progress"><div class="cocina-progress-bar"></div></div>' +
        '</div>';
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
      MammaTomatoAlert.success("Pedido entregado", `${formatId(idPedido)} movido a entregados`);

      // Localmente mutamos el estado a ATENDIDO para que viaje de columna en el render
      const idx = pedidos.findIndex(p => p.idPedido === idPedido);
      if (idx !== -1) pedidos[idx].estado = "ATENDIDO";

      // Forzamos el traspaso inmediato a la columna "Entregados"
      render();

      // Buscamos la tarjeta recién movida en su nueva columna para animarla
      const tarjetaEnEntregados = colEntregados.querySelector(`[data-id="${idPedido}"]`);
      animarAtendido(idPedido, tarjetaEnEntregados);
    } else {
      MammaTomatoAlert.info("Estado actualizado", `${formatId(idPedido)} en preparación`);
      await loadKitchen();
    }
  } catch (err) {
    MammaTomatoAlert.error("No se pudo actualizar el estado", err.message);
    btn.disabled = false;
  }
}

function animarAtendido(idPedido, card) {
  animando.add(idPedido);
  if (!card) {
    setTimeout(() => finalizarSalida(idPedido), SALIDA_MS);
    return;
  }

  setTimeout(() => {
    card.classList.add("saliendo");
    setTimeout(() => {
      finalizarSalida(idPedido, card);
    }, 450); // Tiempo de la transición CSS fading/collapse
  }, SALIDA_MS);
}

function finalizarSalida(idPedido, card) {
  animando.delete(idPedido);
  conocidos.delete(idPedido);
  pedidos = pedidos.filter((p) => p.idPedido !== idPedido);
  if (card) card.remove();
  render();
}

if (refreshBtn) refreshBtn.addEventListener("click", loadKitchen);

loadKitchen();
setInterval(loadKitchen, REFRESH_MS);