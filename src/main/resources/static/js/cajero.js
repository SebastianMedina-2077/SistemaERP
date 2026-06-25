const IGV = 0.18;
const money = (value) => `S/ ${Number(value || 0).toFixed(2)}`;

// --- CSRF (Spring Security) para las peticiones mutadoras ----------
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

async function getJson(url) {
  const res = await fetch(url, { headers: { Accept: "application/json" } });
  if (!res.ok) throw await toError(res);
  return res.json();
}

async function sendJson(method, url, body) {
  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
  const res = await fetch(url, { method, headers, body: JSON.stringify(body) });
  if (!res.ok) throw await toError(res);
  return res.json();
}

async function toError(res) {
  try {
    const data = await res.json();
    return Object.assign(new Error(data.mensaje || res.statusText), { payload: data, status: res.status });
  } catch {
    return Object.assign(new Error(res.statusText), { status: res.status });
  }
}

// --- DOM ------------------------------------------------------------
const categoryButtons = document.querySelector("#categoryButtons");
const productGrid = document.querySelector("#productGrid");
const productSearch = document.querySelector("#productSearch");
const pagePrev = document.querySelector("#pagePrev");
const pageNext = document.querySelector("#pageNext");
const pageInfo = document.querySelector("#pageInfo");
const orderItems = document.querySelector("#orderItems");
const clearOrderBtn = document.querySelector("#clearOrderBtn");
const generateTicketBtn = document.querySelector("#generateTicketBtn");
const stockAlert = document.querySelector("#stockAlert");
const customerName = document.querySelector("#customerName");
const customerPhone = document.querySelector("#customerPhone");
const paymentMethod = document.querySelector("#paymentMethod");
const subtotalNode = document.querySelector("#subtotal");
const igvNode = document.querySelector("#igv");
const totalNode = document.querySelector("#total");
const saveOrderBtn = document.querySelector("#saveOrderBtn");
const badgeGuardados = document.querySelector("#badgeGuardados");

const PAGE_SIZE = 30;
let activeCategory = null;
let order = [];
let catalog = [];
let searchTerm = "";
let currentPage = 0;

// --- Categorias -----------------------------------------------------
function setupCategories() {
  const buttons = [...categoryButtons.querySelectorAll("button")];
  buttons.forEach((button) => {
    button.addEventListener("click", () => {
      activeCategory = Number(button.dataset.category);
      buttons.forEach((b) => {
        const on = Number(b.dataset.category) === activeCategory;
        b.classList.toggle("btn-secondary", on);
        b.classList.toggle("btn-ghost", !on);
      });
      searchTerm = "";
      if (productSearch) productSearch.value = "";
      loadProducts();
    });
  });
  if (buttons.length) activeCategory = Number(buttons[0].dataset.category);
}

// --- Productos ------------------------------------------------------
async function loadProducts() {
  if (activeCategory == null) return;
  productGrid.textContent = "Cargando...";
  try {
    catalog = await getJson(`/api/productos?categoriaId=${activeCategory}`);
    currentPage = 0;
    renderGrid();
  } catch (err) {
    catalog = [];
    productGrid.textContent = `No se pudo cargar el catalogo: ${err.message}`;
    updatePager(0);
  }
}

function filteredProducts() {
  const term = searchTerm.trim().toLowerCase();
  if (!term) return catalog;
  return catalog.filter((p) => p.nombre.toLowerCase().includes(term));
}

function renderGrid() {
  const items = filteredProducts();
  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  if (currentPage > totalPages - 1) currentPage = totalPages - 1;
  if (currentPage < 0) currentPage = 0;

  if (!items.length) {
    productGrid.replaceChildren();
    productGrid.textContent = "Sin productos para esta busqueda.";
    updatePager(0);
    return;
  }

  const start = currentPage * PAGE_SIZE;
  const pageItems = items.slice(start, start + PAGE_SIZE);
  productGrid.replaceChildren(...pageItems.map(renderProductButton));
  updatePager(totalPages);
}

function updatePager(totalPages) {
  if (!totalPages) {
    pageInfo.textContent = "0 de 0";
    pagePrev.disabled = true;
    pageNext.disabled = true;
    return;
  }
  pageInfo.textContent = `${currentPage + 1} de ${totalPages}`;
  pagePrev.disabled = currentPage <= 0;
  pageNext.disabled = currentPage >= totalPages - 1;
}

function renderProductButton(product) {
  const stockText = product.preparado ? "Preparado" : `Stock ${product.stock}`;
  const button = document.createElement("button");
  button.type = "button";
  button.className = "product-btn";

  const nombre = document.createElement("strong");
  nombre.className = "product-btn__name";
  nombre.textContent = product.nombre;

  const precio = document.createElement("span");
  precio.className = "product-btn__price";
  precio.textContent = money(product.precio);

  const stock = document.createElement("span");
  stock.className = "product-btn__stock";
  stock.textContent = stockText;

  button.append(nombre, precio, stock);
  button.addEventListener("click", () => addProduct(product));
  return button;
}

// --- Pedido en curso ------------------------------------------------
async function addProduct(product) {
  const existing = order.find((item) => item.idProducto === product.idProducto);
  const nextQuantity = existing ? existing.cantidad + 1 : 1;

  try {
    const check = await getJson(`/api/stock/check?productoId=${product.idProducto}&cantidad=${nextQuantity}`);
    if (!check.ok) {
      showStockAlert(check.faltantes);
      return;
    }
  } catch (err) {
    showStockAlert(null, err.message);
    return;
  }

  hideStockAlert();
  if (existing) existing.cantidad += 1;
  else order.push({ idProducto: product.idProducto, nombre: product.nombre, precio: Number(product.precio), cantidad: 1, observacion: "" });
  renderOrder();
}

function showStockAlert(faltantes, message) {
  stockAlert.classList.remove("hidden");
  if (message) stockAlert.textContent = `No se pudo verificar el stock: ${message}`;
  else if (faltantes && faltantes.length) stockAlert.textContent = `Stock insuficiente: ${faltantes.map((f) => f.insumo).join(", ")}.`;
  else stockAlert.textContent = "Stock insuficiente para registrar este producto.";
}

function hideStockAlert() {
  stockAlert.classList.add("hidden");
}

function renderOrder() {
  if (!order.length) {
    orderItems.className = "order-items empty-state";
    orderItems.textContent = "No hay productos agregados.";
    renderTotals();
    return;
  }
  orderItems.className = "order-items";
  orderItems.replaceChildren(...order.map((item, index) => renderOrderRow(item, index)));
  renderTotals();
}

function renderOrderRow(item, index) {
  const subtotal = item.precio * item.cantidad;
  const row = document.createElement("div");
  row.className = "order-row";

  const head = document.createElement("div");
  head.className = "order-row-head";
  const name = document.createElement("strong");
  name.textContent = item.nombre;
  const price = document.createElement("span");
  price.textContent = money(subtotal);
  head.append(name, price);

  const qty = document.createElement("div");
  qty.className = "qty-controls";
  const dec = button("-", () => updateQuantity(index, -1));
  const count = document.createElement("strong");
  count.textContent = item.cantidad;
  const inc = button("+", () => updateQuantity(index, 1));
  qty.append(dec, count, inc);

  const label = document.createElement("label");
  label.textContent = "Observacion";
  const obs = document.createElement("input");
  obs.value = item.observacion;
  obs.maxLength = 100;
  obs.placeholder = "Ej. sin cebolla";
  obs.addEventListener("input", () => { item.observacion = obs.value; });
  label.append(obs);

  row.append(head, qty, label);
  return row;
}

function button(text, onClick) {
  const b = document.createElement("button");
  b.type = "button";
  b.textContent = text;
  b.addEventListener("click", onClick);
  return b;
}

async function updateQuantity(index, delta) {
  const item = order[index];
  if (delta > 0) {
    try {
      const check = await getJson(`/api/stock/check?productoId=${item.idProducto}&cantidad=${item.cantidad + 1}`);
      if (!check.ok) { showStockAlert(check.faltantes); return; }
    } catch (err) { showStockAlert(null, err.message); return; }
  }
  item.cantidad += delta;
  order = order.filter((it) => it.cantidad > 0);
  hideStockAlert();
  renderOrder();
}

function getTotals() {
  const subtotal = order.reduce((sum, item) => sum + item.precio * item.cantidad, 0);
  const igv = subtotal * IGV;
  return { subtotal, igv, total: subtotal + igv };
}

function renderTotals() {
  const totals = getTotals();
  subtotalNode.textContent = money(totals.subtotal);
  igvNode.textContent = money(totals.igv);
  totalNode.textContent = money(totals.total);
}

// --- Generar boleta -------------------------------------------------
function marcarError(el) {
  if (!el) return;
  el.classList.add("campo-error");
  el.focus();
  setTimeout(() => el.classList.remove("campo-error"), 2500);
}

async function generateTicket() {
  if (!order.length) {
    MammaTomatoAlert.warning("Pedido vacio", "Agrega al menos un producto al pedido");
    return;
  }
  if (!customerName.value.trim()) {
    MammaTomatoAlert.warning("Datos incompletos", "El nombre del cliente es obligatorio");
    marcarError(customerName);
    return;
  }
  if (!paymentMethod.value) {
    MammaTomatoAlert.warning("Datos incompletos", "Selecciona el metodo de pago");
    marcarError(paymentMethod);
    return;
  }

  if (esEfectivo()) abrirCobro();
  else enviarVenta(null, null);
}

function esEfectivo() {
  const opcion = paymentMethod.options[paymentMethod.selectedIndex];
  return opcion ? /efectivo/i.test(opcion.textContent) : false;
}

async function enviarVenta(vuelto, pagos, desglose) {
  const payload = {
    clienteNombre: customerName.value.trim(),
    clienteTelefono: customerPhone.value.trim() || null,
    idMetodoPago: Number(paymentMethod.value),
    items: order.map((item) => ({ idProducto: item.idProducto, cantidad: item.cantidad, observacion: item.observacion || null })),
    pagos: pagos && pagos.length ? pagos : null,
  };

  const totalItems = order.reduce((sum, item) => sum + item.cantidad, 0);
  const eta = 10 + totalItems * 2;

  generateTicketBtn.disabled = true;
  try {
    const boleta = await sendJson("POST", "/api/pedidos", payload);
    showOrderConfirm(boleta, eta, vuelto, desglose);
    clearOrder();
  } catch (err) {
    const faltantes = err.payload?.detalle;
    if (Array.isArray(faltantes) && faltantes.length && faltantes[0].insumo) {
      showStockAlert(faltantes);
    } else {
      MammaTomatoAlert.error("No se pudo generar la boleta", err.message);
    }
  } finally {
    generateTicketBtn.disabled = false;
  }
}

function clearOrder() {
  order = [];
  customerName.value = "";
  customerPhone.value = "";
  hideStockAlert();
  renderOrder();
}

// --- Guardar pedido en espera (almacen en memoria del servidor) -----
function guardarPedido() {
  if (!order.length) {
    MammaTomatoAlert.warning("Pedido vacio", "Agrega al menos un producto antes de guardar");
    return;
  }
  const nombre = customerName.value.trim() || "Sin nombre";
  mostrarModalConfirmacion({
    titulo: "Guardar pedido en espera",
    mensaje: `Guardar el pedido de "${nombre}" para retomarlo despues?`,
    labelConfirmar: "Guardar",
    labelCancelar: "Cancelar",
    onConfirmar: ejecutarGuardarPedido,
  });
}

async function ejecutarGuardarPedido() {
  const totals = getTotals();
  const payload = {
    cliente: customerName.value.trim() || "Sin nombre",
    telefono: customerPhone.value.trim() || null,
    idMetodoPago: paymentMethod.value ? Number(paymentMethod.value) : null,
    items: order.map((item) => ({
      idProducto: item.idProducto,
      nombre: item.nombre,
      precio: item.precio,
      cantidad: item.cantidad,
      observacion: item.observacion || null,
    })),
    subtotal: totals.subtotal,
    total: totals.total,
  };

  saveOrderBtn.disabled = true;
  try {
    const guardado = await sendJson("POST", "/cajero/pedidos/guardar", payload);
    MammaTomatoAlert.success("Pedido guardado", `Guardado como "${guardado.referencia}"`);
    clearOrder();
    actualizarBadgeGuardados();
  } catch (err) {
    MammaTomatoAlert.error("Error al guardar", err.message);
  } finally {
    saveOrderBtn.disabled = false;
  }
}

async function actualizarBadgeGuardados() {
  if (!badgeGuardados) return;
  try {
    const data = await getJson("/cajero/pedidos/guardados/count");
    const n = data.count || 0;
    badgeGuardados.textContent = n;
    badgeGuardados.hidden = n === 0;
  } catch {
    /* silencioso: el badge es informativo */
  }
}

// --- Recuperar un pedido guardado (llega desde /cajero/guardados) ----
function cargarPedidoRecuperado() {
  const raw = sessionStorage.getItem("mt-pedido-recuperado");
  if (!raw) return;
  sessionStorage.removeItem("mt-pedido-recuperado");
  try {
    const pedido = JSON.parse(raw);
    order = (pedido.items || []).map((i) => ({
      idProducto: i.idProducto,
      nombre: i.nombre,
      precio: Number(i.precio),
      cantidad: i.cantidad,
      observacion: i.observacion || "",
    }));
    customerName.value = pedido.cliente && pedido.cliente !== "Sin nombre" ? pedido.cliente : "";
    customerPhone.value = pedido.telefono || "";
    if (pedido.idMetodoPago != null) paymentMethod.value = String(pedido.idMetodoPago);
    renderOrder();
    MammaTomatoAlert.info("Pedido recuperado", "El pedido fue cargado al carrito");
  } catch {
    /* ignora json invalido */
  }
}

// --- Confirmacion de orden -----------------------------------------
const orderConfirm = document.querySelector("#orderConfirm");
const confirmTitle = document.querySelector("#confirmTitle");
const confirmEta = document.querySelector("#confirmEta");
const confirmDismiss = document.querySelector("#confirmDismiss");
const confirmNew = document.querySelector("#confirmNew");

const cobroModal = document.querySelector("#cobroModal");
const cobroTotalNode = document.querySelector("#cobroTotal");
const cobroRecibido = document.querySelector("#cobroRecibido");
const cobroVueltoNode = document.querySelector("#cobroVuelto");
const cobroVueltoBox = document.querySelector("#cobroVueltoBox");
const cobroConfirm = document.querySelector("#cobroConfirm");
const cobroExacto = document.querySelector("#cobroExacto");
const cobroClose = document.querySelector("#cobroClose");
const cobroReset = document.querySelector("#cobroReset");
const cobroMixto = document.querySelector("#cobroMixto");
const cobroMixtoBlock = document.querySelector("#cobroMixtoBlock");
const cobroMetodo2 = document.querySelector("#cobroMetodo2");
const cobroMonto2 = document.querySelector("#cobroMonto2");

function showOrderConfirm(boleta, eta, vuelto, desglose) {
  const numero = boleta.idPedido != null ? boleta.idPedido : boleta.numeroBoleta;
  confirmTitle.textContent = `Orden #${numero} enviada a cocina`;
  let info = `<i class="bi bi-clock"></i> Tiempo estimado: ${eta} min.`;
  if (Array.isArray(desglose) && desglose.length) {
    const partes = desglose.map((p) => `${p.metodo} ${money(p.monto)}`).join(" + ");
    info += `<br><i class="bi bi-wallet2"></i> Pago: ${partes}`;
  }
  if (vuelto != null && vuelto > 0.001) {
    info += `<br><i class="bi bi-cash-coin"></i> Vuelto: ${money(vuelto)}`;
  }
  confirmEta.innerHTML = info;
  orderConfirm.classList.remove("hidden");
}

function hideOrderConfirm() {
  orderConfirm.classList.add("hidden");
}

confirmDismiss.addEventListener("click", hideOrderConfirm);
confirmNew.addEventListener("click", hideOrderConfirm);
orderConfirm.addEventListener("click", (e) => { if (e.target === orderConfirm) hideOrderConfirm(); });

// --- Cobro en efectivo + vuelto -------------------------------------
let cobroTotal = 0;

function abrirCobro() {
  cobroTotal = getTotals().total;
  cobroTotalNode.textContent = money(cobroTotal);
  cobroRecibido.value = "";
  cobroMixto.checked = false;
  cobroMetodo2.value = "";
  cobroMonto2.value = "";
  cobroMixtoBlock.classList.add("hidden");
  actualizarVuelto();
  cobroModal.classList.remove("hidden");
  setTimeout(() => cobroRecibido.focus(), 50);
}

function cerrarCobro() {
  cobroModal.classList.add("hidden");
}

// En pago mixto, parte del total va a otro metodo; el resto se cobra en efectivo.
function montoDigital() {
  return cobroMixto.checked ? Number(cobroMonto2.value) || 0 : 0;
}

function porcionEfectivo() {
  return Math.max(0, cobroTotal - montoDigital());
}

function actualizarVuelto() {
  const efectivo = porcionEfectivo();
  const recibido = Number(cobroRecibido.value) || 0;
  const vuelto = recibido - efectivo;
  const falta = recibido > 0 && vuelto < -0.001;
  cobroVueltoBox.classList.toggle("falta", falta);
  cobroVueltoNode.textContent = falta ? `Falta ${money(Math.abs(vuelto))}` : money(Math.max(0, vuelto));
  cobroConfirm.disabled = !cobroPuedeConfirmar(recibido, efectivo);
}

// Confirmable si cubre el efectivo y, en mixto, el segundo metodo es valido (>0 y < total).
function cobroPuedeConfirmar(recibido, efectivo) {
  if (recibido + 0.001 < efectivo) return false;
  if (!cobroMixto.checked) return true;
  const digital = montoDigital();
  return Boolean(cobroMetodo2.value) && digital > 0.001 && digital + 0.001 < cobroTotal;
}

cobroRecibido.addEventListener("input", actualizarVuelto);

document.querySelectorAll(".cobro-billete[data-monto]").forEach((boton) => {
  boton.addEventListener("click", () => {
    const actual = Number(cobroRecibido.value) || 0;
    cobroRecibido.value = (actual + Number(boton.dataset.monto)).toFixed(2);
    actualizarVuelto();
  });
});

cobroExacto.addEventListener("click", () => {
  cobroRecibido.value = porcionEfectivo().toFixed(2);
  actualizarVuelto();
});

cobroMixto.addEventListener("change", () => {
  cobroMixtoBlock.classList.toggle("hidden", !cobroMixto.checked);
  if (!cobroMixto.checked) { cobroMetodo2.value = ""; cobroMonto2.value = ""; }
  actualizarVuelto();
});
cobroMetodo2.addEventListener("change", actualizarVuelto);
cobroMonto2.addEventListener("input", actualizarVuelto);

cobroReset.addEventListener("click", () => {
  cobroRecibido.value = "";
  actualizarVuelto();
  cobroRecibido.focus();
});

cobroClose.addEventListener("click", cerrarCobro);
cobroModal.addEventListener("click", (e) => { if (e.target === cobroModal) cerrarCobro(); });

function metodoLabel(select) {
  const opcion = select.options[select.selectedIndex];
  return opcion ? opcion.textContent.trim() : "";
}

cobroConfirm.addEventListener("click", () => {
  const efectivo = porcionEfectivo();
  const vuelto = (Number(cobroRecibido.value) || 0) - efectivo;
  let pagos = null;
  let desglose = null;
  if (cobroMixto.checked) {
    const efectivoMonto = Number(efectivo.toFixed(2));
    const digitalMonto = Number(montoDigital().toFixed(2));
    pagos = [
      { idMetodoPago: Number(paymentMethod.value), monto: efectivoMonto },
      { idMetodoPago: Number(cobroMetodo2.value), monto: digitalMonto },
    ];
    desglose = [
      { metodo: metodoLabel(paymentMethod), monto: efectivoMonto },
      { metodo: metodoLabel(cobroMetodo2), monto: digitalMonto },
    ];
  }
  cerrarCobro();
  enviarVenta(vuelto, pagos, desglose);
});

// --- Atajos de teclado ----------------------------------------------
document.addEventListener("keydown", (e) => {
  if (!cobroModal.classList.contains("hidden")) {
    if (e.key === "Escape") { e.preventDefault(); cerrarCobro(); }
    else if (e.key === "Enter" && !cobroConfirm.disabled) { e.preventDefault(); cobroConfirm.click(); }
    return;
  }
  if (!orderConfirm.classList.contains("hidden")) {
    if (e.key === "Escape" || e.key === "Enter") { e.preventDefault(); hideOrderConfirm(); }
    return;
  }
  const enCampo = ["INPUT", "SELECT", "TEXTAREA"].includes(document.activeElement?.tagName);
  if (e.key === "/" && !enCampo) { e.preventDefault(); productSearch.focus(); }
  else if (e.key === "F9") { e.preventDefault(); generateTicket(); }
  else if (e.key === "Escape" && enCampo) { document.activeElement.blur(); }
});

// --- Buscador + paginacion -----------------------------------------
productSearch.addEventListener("input", () => {
  searchTerm = productSearch.value;
  currentPage = 0;
  renderGrid();
});

pagePrev.addEventListener("click", () => { currentPage -= 1; renderGrid(); });
pageNext.addEventListener("click", () => { currentPage += 1; renderGrid(); });

clearOrderBtn.addEventListener("click", clearOrder);
generateTicketBtn.addEventListener("click", generateTicket);
saveOrderBtn.addEventListener("click", guardarPedido);

setupCategories();
loadProducts();
cargarPedidoRecuperado();
renderOrder();
actualizarBadgeGuardados();
