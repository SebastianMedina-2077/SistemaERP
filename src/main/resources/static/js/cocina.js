// Respaldo lento: el refresco real llega por SSE; este intervalo solo cubre una
// posible caida del stream.
const REFRESH_MS = 30000;

// Gracia visual antes de dar un pedido por entregado (ATENDIDO). La barra de la
// columna Entregados se consume en este tiempo; al agotarse se confirma la entrega.
const DURACION_ENTREGA = 10000;

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

// ===== Barra de entrega persistente (sobrevive re-render / SSE) =====
// idPedido -> timestamp (Date.now()) en que arranco su gracia de entrega.
const entregasEnCurso = new Map();
// idPedido -> true cuando ya se disparo el PATCH de confirmacion (evita repetirlo).
const entregasConfirmadas = new Set();

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

// Las bebidas no pasan por el horno: se atenuan para dejar claro que no requieren preparacion.
function esBebida(categoria) {
  return String(categoria || "").trim().toLowerCase() === "bebidas";
}

// Un PREPARANDO con todos los items servidos ya no es "cocina": pasa a fase de entrega.
function todosServidos(pedido) {
  const items = pedido.items || [];
  return items.length > 0 && items.every((i) => i.servido);
}

async function loadKitchen() {
  try {
    const res = await fetch("/api/pedidos/cocina", { headers: { Accept: "application/json" } });
    if (!res.ok) throw new Error(res.statusText);

    const datosNuevos = await res.json();
    detectarNuevos(datosNuevos);

    pedidos = datosNuevos;
    render();
  } catch (err) {
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
      // El backend solo trae PENDIENTE/PREPARANDO; un id no conocido es un pedido entrante.
      if (!conocidos.has(p.idPedido)) {
        MammaTomatoAlert.info("Nuevo pedido recibido", `${formatId(p.idPedido)} · ${p.cliente ?? "Cliente"}`, 6000);
      }
    });
  }
  conocidos = new Set(lista.map((p) => p.idPedido));
  primeraCarga = false;
}

function actualizarContadores(pendientes, preparando, entregados) {
  // La "cola" son los pedidos que aun exigen trabajo en cocina (pendientes + en preparacion).
  if (colaCount) colaCount.textContent = pendientes.length + preparando.length;
  if (countPendientes) countPendientes.textContent = pendientes.length;
  if (countPreparando) countPreparando.textContent = preparando.length;
  if (countEntregados) countEntregados.textContent = entregados.length;
}

function render() {
  // Ordenar respetando el orden cronológico de llegada
  const pedidosOrdenados = [...pedidos].sort((a, b) => new Date(a.fecha) - new Date(b.fecha));

  // La columna se DERIVA (no del estado directo): un PREPARANDO puede estar en preparacion o
  // en entrega segun si ya se sirvieron todos sus items.
  const listaPendientes = pedidosOrdenados.filter((p) => String(p.estado).toUpperCase() === "PENDIENTE");
  const listaPreparando = pedidosOrdenados.filter(
    (p) => String(p.estado).toUpperCase() === "PREPARANDO" && !todosServidos(p)
  );
  // Los entregados mas recientes primero: son los que el usuario podria querer deshacer.
  const listaEntregados = pedidosOrdenados
    .filter((p) => String(p.estado).toUpperCase() === "PREPARANDO" && todosServidos(p))
    .reverse();

  sincronizarEntregas(listaEntregados);
  actualizarContadores(listaPendientes, listaPreparando, listaEntregados);

  if (!listaPendientes.length) {
    colPendientes.replaceChildren(estadoVacio("Sin pedidos pendientes"));
  } else {
    colPendientes.replaceChildren(...listaPendientes.map((p) => renderCard(p, "PENDIENTE")));
  }

  if (!listaPreparando.length) {
    colPreparando.replaceChildren(estadoVacio("Sin platos en preparación"));
  } else {
    colPreparando.replaceChildren(...listaPreparando.map((p) => renderCard(p, "PREPARANDO")));
  }

  if (!listaEntregados.length) {
    colEntregados.replaceChildren(estadoVacio("Sin entregas en curso"));
  } else {
    colEntregados.replaceChildren(...listaEntregados.map((p) => renderCard(p, "ENTREGADO")));
  }
}

// Arranca la gracia de los entregados nuevos y limpia el Map de los que ya no aplican, para
// que la barra continue desde donde iba pese a los re-render de loadKitchen/SSE.
function sincronizarEntregas(listaEntregados) {
  const idsEntregados = new Set(listaEntregados.map((p) => p.idPedido));

  // Registrar los que entran a fase de entrega y aun no tienen barra en curso.
  listaEntregados.forEach((p) => {
    if (!entregasEnCurso.has(p.idPedido) && !entregasConfirmadas.has(p.idPedido)) {
      entregasEnCurso.set(p.idPedido, Date.now());
    }
  });

  // Quitar del Map los pedidos que dejaron de estar "todos servidos" o desaparecieron.
  for (const id of [...entregasEnCurso.keys()]) {
    if (!idsEntregados.has(id)) entregasEnCurso.delete(id);
  }

  // Limpiar confirmadas ya ausentes del tablero (el server los saco tras ATENDIDO).
  const idsTablero = new Set(pedidos.map((p) => p.idPedido));
  for (const id of [...entregasConfirmadas]) {
    if (!idsTablero.has(id)) entregasConfirmadas.delete(id);
  }
}

function estadoVacio(mensaje) {
  const box = document.createElement("div");
  box.className = "cocina-vacio";
  box.innerHTML = `<i class="bi bi-check2-all"></i><p>${mensaje}</p>`;
  return box;
}

// vista: "PENDIENTE" | "PREPARANDO" | "ENTREGADO" (columna derivada, no el estado crudo).
function renderCard(pedido, vista) {
  const estado = String(pedido.estado).toUpperCase();
  const min = minutosEspera(pedido.fecha);
  const card = document.createElement("article");
  const vistaClase = vista === "ENTREGADO" ? "estado-entregado" : `estado-${estado.toLowerCase()}`;
  // La urgencia (semaforo) solo aplica mientras el pedido sigue en cocina, no en entrega.
  const urgencia = vista === "ENTREGADO" ? "" : claseUrgencia(min);
  card.className = `cocina-pedido-card ${vistaClase} ${urgencia}`.trim();
  card.dataset.id = pedido.idPedido;

  // Cabecera: identificador + tiempo en cola
  const header = document.createElement("div");
  header.className = "cocina-card-header";
  const id = document.createElement("span");
  id.className = "cocina-card-id";
  id.textContent = formatId(pedido.idPedido);
  const tiempo = document.createElement("span");
  tiempo.className = "cocina-card-tiempo";
  tiempo.textContent = textoEspera(min);
  header.append(id, tiempo);

  // Cliente + insignias (estado y estimacion de horno)
  const cliente = document.createElement("div");
  cliente.className = "cocina-card-cliente";
  const nombre = document.createElement("strong");
  nombre.textContent = pedido.cliente ?? "Sin nombre";

  const badges = document.createElement("div");
  badges.className = "cocina-card-badges";
  const etiqueta = document.createElement("span");
  if (vista === "ENTREGADO") {
    etiqueta.className = "badge-estado-entregado";
    etiqueta.innerHTML = `<i class="bi bi-check2-circle" aria-hidden="true"></i> Pedido entregado`;
  } else {
    etiqueta.className = `badge-estado-${estado.toLowerCase()}`;
    etiqueta.textContent = estado === "PENDIENTE" ? "Pendiente" : "Preparando";
  }
  badges.append(etiqueta);

  if (vista !== "ENTREGADO" && pedido.tiempoEstimadoMin != null) {
    const horno = document.createElement("span");
    horno.className = "cocina-card-horno";
    horno.innerHTML = `<i class="bi bi-fire" aria-hidden="true"></i> ~${pedido.tiempoEstimadoMin} min`;
    horno.setAttribute("title", "Tiempo estimado de horno");
    badges.append(horno);
  }
  cliente.append(nombre, badges);

  // Lista de platos con control de check tipo lista de compras
  const items = document.createElement("div");
  items.className = "cocina-card-items";
  (pedido.items || []).forEach((it) => items.append(renderItem(pedido, it, vista)));

  // Pie: accion segun columna
  const footer = document.createElement("div");
  footer.className = "cocina-card-footer";

  if (vista === "PENDIENTE") {
    footer.append(accionEstadoBtn("Empezar a preparar", "bi-fire", "accion-preparar", pedido.idPedido, "PREPARANDO"));
  } else if (vista === "PREPARANDO") {
    footer.append(progresoFooter(pedido));
  } else {
    footer.append(entregaFooter(pedido));
  }

  card.append(header, cliente, items, footer);
  return card;
}

function renderItem(pedido, it, vista) {
  const servido = !!it.servido || vista === "ENTREGADO";

  const row = document.createElement("div");
  row.className = "cocina-item";
  row.dataset.detalle = it.idDetalle;
  if (servido) row.classList.add("servido");
  if (esBebida(it.categoria)) row.classList.add("es-bebida");

  const etiquetaItem = `${it.cantidad}× ${it.producto}`;

  // Check accesible: boton con rol checkbox (Enter/Espacio nativos)
  const check = document.createElement("button");
  check.type = "button";
  check.className = "cocina-item-check";
  check.setAttribute("role", "checkbox");
  check.setAttribute("aria-checked", servido ? "true" : "false");
  const icono = document.createElement("i");
  icono.className = "bi " + (servido ? "bi-check-square-fill" : "bi-square");
  icono.setAttribute("aria-hidden", "true");
  check.append(icono);

  // Marcar/desmarcar solo se permite en preparacion (el backend rechaza en PENDIENTE con 400).
  if (vista === "PREPARANDO") {
    check.setAttribute("aria-label", `${servido ? "Desmarcar" : "Marcar como servido"}: ${etiquetaItem}`);
    check.addEventListener("click", () => toggleServido(pedido, it, row, check));
  } else {
    check.disabled = true;
    check.setAttribute("aria-disabled", "true");
    if (vista === "PENDIENTE") {
      check.title = "Empieza a preparar para marcar";
      check.setAttribute("aria-label", `Pendiente de preparación: ${etiquetaItem}`);
    } else {
      check.setAttribute("aria-label", `Servido: ${etiquetaItem}`);
    }
  }

  const cantidad = document.createElement("span");
  cantidad.className = "cocina-item-qty";
  cantidad.textContent = `${it.cantidad}x`;

  const texto = document.createElement("div");
  texto.className = "cocina-item-texto";
  const producto = document.createElement("span");
  producto.className = "cocina-item-nombre";
  producto.textContent = it.producto;
  texto.append(producto);
  if (it.tamanio) {
    const tam = document.createElement("span");
    tam.className = "cocina-item-tam";
    tam.textContent = it.tamanio;
    texto.append(tam);
  }
  if (it.observacion) {
    const observacion = document.createElement("span");
    observacion.className = "cocina-item-obs";
    observacion.textContent = it.observacion;
    texto.append(observacion);
  }

  row.append(check, cantidad, texto);
  return row;
}

function progresoFooter(pedido) {
  const total = (pedido.items || []).length;
  const hechos = (pedido.items || []).filter((i) => i.servido).length;

  const wrap = document.createElement("div");
  wrap.className = "cocina-card-progreso";
  wrap.setAttribute("role", "progressbar");
  wrap.setAttribute("aria-label", "Platos servidos");
  wrap.setAttribute("aria-valuemin", "0");
  wrap.setAttribute("aria-valuemax", String(total));
  wrap.setAttribute("aria-valuenow", String(hechos));

  const label = document.createElement("div");
  label.className = "cocina-progreso-label";
  const texto = document.createElement("span");
  texto.innerHTML = `<i class="bi bi-list-check" aria-hidden="true"></i> Platos servidos`;
  const num = document.createElement("span");
  num.className = "cocina-progreso-num";
  num.textContent = `${hechos}/${total}`;
  label.append(texto, num);

  const track = document.createElement("div");
  track.className = "cocina-progreso-track";
  const fill = document.createElement("div");
  fill.className = "cocina-progreso-fill";
  fill.style.width = total ? `${(hechos / total) * 100}%` : "0%";
  track.append(fill);

  wrap.append(label, track);
  return wrap;
}

// Pie de la columna Entregados: barra de gracia que se consume + boton Deshacer.
function entregaFooter(pedido) {
  const confirmada = entregasConfirmadas.has(pedido.idPedido);

  const wrap = document.createElement("div");
  wrap.className = "cocina-entrega";

  const label = document.createElement("div");
  label.className = "cocina-entrega-label";
  label.innerHTML = `<span><i class="bi bi-hourglass-split" aria-hidden="true"></i> Confirmando entrega…</span>`;

  const barra = document.createElement("div");
  barra.className = "cocina-entrega-barra";
  barra.setAttribute("role", "progressbar");
  barra.setAttribute("aria-label", "Tiempo antes de dar el pedido por entregado");
  barra.setAttribute("aria-valuemin", "0");
  barra.setAttribute("aria-valuemax", String(Math.round(DURACION_ENTREGA / 1000)));

  const fill = document.createElement("div");
  fill.className = "cocina-entrega-fill";
  // Ancho inicial derivado del tiempo ya transcurrido: la barra no se reinicia en cada render.
  fill.style.width = `${porcentajeEntrega(pedido.idPedido, confirmada)}%`;
  barra.append(fill);

  const btn = document.createElement("button");
  btn.type = "button";
  btn.className = "btn-cocina-accion accion-deshacer";
  const icono = document.createElement("i");
  icono.className = "bi bi-arrow-counterclockwise";
  icono.setAttribute("aria-hidden", "true");
  btn.append(icono, document.createTextNode(" Deshacer"));
  btn.addEventListener("click", () => reabrirPedido(pedido.idPedido, btn));

  wrap.append(label, barra, btn);
  return wrap;
}

// Porcentaje restante de la barra (100 llena, 0 vacia) segun el tiempo transcurrido.
function porcentajeEntrega(idPedido, confirmada) {
  if (confirmada) return 0;
  const inicio = entregasEnCurso.get(idPedido);
  if (inicio == null) return 100;
  const restante = DURACION_ENTREGA - (Date.now() - inicio);
  return Math.max(0, Math.min(100, (restante / DURACION_ENTREGA) * 100));
}

// Ticker unico: refresca todas las barras visibles y confirma la entrega al agotarse.
function tickEntregas() {
  if (!entregasEnCurso.size || !colEntregados) return;
  const ahora = Date.now();
  entregasEnCurso.forEach((inicio, id) => {
    const restante = DURACION_ENTREGA - (ahora - inicio);
    const pct = Math.max(0, Math.min(100, (restante / DURACION_ENTREGA) * 100));
    const card = colEntregados.querySelector(`[data-id="${id}"]`);
    if (card) {
      const fill = card.querySelector(".cocina-entrega-fill");
      if (fill) fill.style.width = `${pct}%`;
      const barra = card.querySelector(".cocina-entrega-barra");
      if (barra) barra.setAttribute("aria-valuenow", String(Math.ceil(Math.max(0, restante) / 1000)));
    }
    if (restante <= 0) {
      entregasEnCurso.delete(id);
      confirmarEntrega(id);
    }
  });
}

function actualizarProgreso(pedido) {
  const card = colPreparando.querySelector(`[data-id="${pedido.idPedido}"]`);
  if (!card) return;
  const wrap = card.querySelector(".cocina-card-progreso");
  if (!wrap) return;
  const total = (pedido.items || []).length;
  const hechos = (pedido.items || []).filter((i) => i.servido).length;
  const num = wrap.querySelector(".cocina-progreso-num");
  const fill = wrap.querySelector(".cocina-progreso-fill");
  if (num) num.textContent = `${hechos}/${total}`;
  if (fill) fill.style.width = total ? `${(hechos / total) * 100}%` : "0%";
  wrap.setAttribute("aria-valuenow", String(hechos));
}

// Marca/desmarca un plato como servido. Solo se llama desde la columna En Preparación.
async function toggleServido(pedido, it, row, check) {
  const nuevo = !it.servido;
  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

  check.disabled = true;
  row.classList.add("cargando");
  try {
    const res = await fetch(`/api/pedidos/${pedido.idPedido}/items/${it.idDetalle}/servido`, {
      method: "PATCH",
      headers,
      body: JSON.stringify({ servido: nuevo }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      const msg = data.mensaje || (res.status === 400 ? "Primero empieza a preparar el pedido" : res.statusText);
      throw new Error(msg);
    }

    const data = await res.json(); // { idPedido, servidoActualizado, todosServidos, nuevoEstado }
    it.servido = data.servidoActualizado;
    aplicarServidoDOM(row, check, it);
    row.classList.remove("cargando");

    if (data.todosServidos) {
      // El pedido se queda en PREPARANDO, pero pasa a la columna Entregados (fase de entrega).
      pasarAEntrega(pedido);
    } else {
      actualizarProgreso(pedido);
      check.disabled = false;
    }
  } catch (err) {
    row.classList.remove("cargando");
    check.disabled = false;
    MammaTomatoAlert.error("No se pudo marcar el plato", err.message);
  }
}

function aplicarServidoDOM(row, check, it) {
  const servido = !!it.servido;
  row.classList.toggle("servido", servido);
  check.setAttribute("aria-checked", servido ? "true" : "false");
  const nombre = row.querySelector(".cocina-item-nombre")?.textContent ?? "";
  check.setAttribute("aria-label", `${servido ? "Desmarcar" : "Marcar como servido"}: ${it.cantidad}× ${nombre}`);
  const icono = check.querySelector("i");
  if (icono) icono.className = "bi " + (servido ? "bi-check-square-fill" : "bi-square");
}

// Todos los platos servidos: animamos la salida de En Preparación y re-renderizamos para que
// la card reaparezca en Entregados con su barra de gracia (sincronizarEntregas la registra).
function pasarAEntrega(pedido) {
  MammaTomatoAlert.success("Pedido servido completo", `${formatId(pedido.idPedido)} pasa a entrega`);
  const card = colPreparando.querySelector(`[data-id="${pedido.idPedido}"]`);
  if (card) {
    card.classList.add("completado");
    setTimeout(render, 300);
  } else {
    render();
  }
}

// Gracia agotada: se confirma la ENTREGA (ATENDIDO) una sola vez y el pedido sale del tablero.
async function confirmarEntrega(idPedido) {
  if (entregasConfirmadas.has(idPedido)) return;
  entregasConfirmadas.add(idPedido);

  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

  const card = colEntregados.querySelector(`[data-id="${idPedido}"]`);
  if (card) card.classList.add("completado");

  try {
    const res = await fetch(`/api/pedidos/${idPedido}/estado`, {
      method: "PATCH",
      headers,
      body: JSON.stringify({ estado: "ATENDIDO" }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.mensaje || res.statusText);
    }
    MammaTomatoAlert.success("Pedido entregado", `${formatId(idPedido)} entregado al cliente`);
    setTimeout(loadKitchen, 420);
  } catch (err) {
    // Falló la confirmacion: permitimos reintento en el proximo render.
    entregasConfirmadas.delete(idPedido);
    if (card) card.classList.remove("completado");
    MammaTomatoAlert.error("No se pudo confirmar la entrega", err.message);
    loadKitchen();
  }
}

// Deshacer: reabre un PREPARANDO ya servido, desmarca TODOS sus items y lo devuelve a preparacion.
async function reabrirPedido(idPedido, btn) {
  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

  btn.disabled = true;
  const card = colEntregados.querySelector(`[data-id="${idPedido}"]`);
  try {
    const res = await fetch(`/api/pedidos/${idPedido}/reabrir`, { method: "PATCH", headers });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.mensaje || res.statusText);
    }

    // Cancela la barra y refleja el desmarcado local antes de resincronizar con el server.
    entregasEnCurso.delete(idPedido);
    entregasConfirmadas.delete(idPedido);
    const pedido = pedidos.find((p) => p.idPedido === idPedido);
    if (pedido) (pedido.items || []).forEach((i) => (i.servido = false));

    MammaTomatoAlert.info("Pedido reabierto", `${formatId(idPedido)} vuelve a preparación`);
    if (card) {
      card.classList.add("completado");
      setTimeout(loadKitchen, 320);
    } else {
      loadKitchen();
    }
  } catch (err) {
    btn.disabled = false;
    MammaTomatoAlert.error("No se pudo reabrir el pedido", err.message);
  }
}

function accionEstadoBtn(text, iconClass, clase, idPedido, estado) {
  const b = document.createElement("button");
  b.type = "button";
  b.className = `btn-cocina-accion ${clase}`;
  const icono = document.createElement("i");
  icono.className = `bi ${iconClass}`;
  icono.setAttribute("aria-hidden", "true");
  b.append(icono, document.createTextNode(` ${text}`));
  b.addEventListener("click", () => updateStatus(idPedido, estado, b));
  return b;
}

// Cambia el estado del pedido. Hoy solo se usa para Empezar a preparar (PENDIENTE->PREPARANDO).
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
    MammaTomatoAlert.info("Estado actualizado", `${formatId(idPedido)} en preparación`);
    await loadKitchen();
  } catch (err) {
    MammaTomatoAlert.error("No se pudo actualizar el estado", err.message);
    btn.disabled = false;
  }
}

if (refreshBtn) refreshBtn.addEventListener("click", loadKitchen);

// Tiempo real: refresca al instante cuando entra un pedido nuevo o cambia un estado.
if (window.MammaTomatoRealtime) {
  window.MammaTomatoRealtime.on("pedido-nuevo", loadKitchen);
  window.MammaTomatoRealtime.on("pedido-estado", loadKitchen);
}

// realtime.js no reexpone el evento "pedido-item", asi que abrimos un canal SSE propio
// solo para refrescar la raya de servido en vivo (util cuando hay otra pantalla de cocina).
if (window.EventSource) {
  try {
    const canalServido = new EventSource("/api/eventos");
    canalServido.addEventListener("pedido-item", () => loadKitchen());
  } catch (_) {
    /* sin SSE seguimos con el polling de respaldo */
  }
}

loadKitchen();
setInterval(loadKitchen, REFRESH_MS);
// Un unico ticker mueve todas las barras de entrega y dispara la confirmacion al agotarse.
setInterval(tickEntregas, 200);
