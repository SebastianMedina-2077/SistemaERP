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
const descuentoRow = document.querySelector("#descuentoRow");
const descuentoNode = document.querySelector("#descuento");
const saveOrderBtn = document.querySelector("#saveOrderBtn");
const badgeGuardados = document.querySelector("#badgeGuardados");

const PAGE_SIZE = 30;
let activeCategory = null;
let order = [];
let catalog = [];
let searchTerm = "";
let currentPage = 0;
let cotizacion = null; // total autoritativo del backend (con descuentos de promocion)

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
  else order.push({ idProducto: product.idProducto, nombre: product.nombre, precio: Number(product.precio), cantidad: 1, observacion: "", categoria: product.categoria || "" });
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
  programarCotizacion();
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

  const controlesCantidad = document.createElement("div");
  controlesCantidad.className = "qty-controls";
  const dec = button("-", () => updateQuantity(index, -1));
  const count = document.createElement("strong");
  count.textContent = item.cantidad;
  const inc = button("+", () => updateQuantity(index, 1));
  controlesCantidad.append(dec, count, inc);

  const label = document.createElement("label");
  label.textContent = "Observacion";
  const observacion = document.createElement("input");
  observacion.value = item.observacion;
  observacion.maxLength = 100;
  observacion.placeholder = "Ej. sin cebolla";
  observacion.addEventListener("input", () => { item.observacion = observacion.value; });
  label.append(observacion);

  row.append(head, controlesCantidad, label);
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

// Firma del pedido para saber si la cotizacion en cache sigue vigente.
function firmaOrden() {
  return order.map((i) => `${i.idProducto}x${i.cantidad}`).join("|");
}

function getTotals() {
  // Preferimos el total autoritativo del backend, que aplica los descuentos de
  // promocion (precios con IGV incluido; igv = total x 18/118). Si aun no llego,
  // caemos a la suma local para no dejar la UI en blanco.
  if (cotizacion && cotizacion.firma === firmaOrden()) {
    return {
      subtotal: Number(cotizacion.subtotal),
      igv: Number(cotizacion.igv),
      total: Number(cotizacion.total),
    };
  }
  const total = order.reduce((sum, item) => sum + item.precio * item.cantidad, 0);
  const igv = (total * IGV) / (1 + IGV);
  const subtotal = total - igv;
  return { subtotal, igv, total };
}

function totalDescuento() {
  if (!cotizacion || cotizacion.firma !== firmaOrden()) return 0;
  return (cotizacion.lineas || []).reduce((sum, l) => sum + Number(l.descuento || 0), 0);
}

function renderTotals() {
  const totals = getTotals();
  subtotalNode.textContent = money(totals.subtotal);
  igvNode.textContent = money(totals.igv);
  totalNode.textContent = money(totals.total);
  const desc = totalDescuento();
  if (descuentoRow) descuentoRow.classList.toggle("hidden", desc <= 0.001);
  if (descuentoNode) descuentoNode.textContent = `- ${money(desc)}`;
}

// Pide al backend el total con descuentos y refresca la vista. Sin conexion,
// getTotals() cae al calculo local (el backend sigue siendo el arbitro final).
async function refrescarCotizacion() {
  const firma = firmaOrden();
  if (!order.length) {
    cotizacion = null;
    renderTotals();
    return;
  }
  try {
    const data = await sendJson("POST", "/api/pedidos/cotizar", {
      items: order.map((i) => ({ idProducto: i.idProducto, cantidad: i.cantidad })),
    });
    cotizacion = { ...data, firma };
  } catch {
    cotizacion = null;
  }
  renderTotals();
}

let cotizarTimer = null;
function programarCotizacion() {
  clearTimeout(cotizarTimer);
  cotizarTimer = setTimeout(refrescarCotizacion, 250);
}

// Fuerza una cotizacion fresca antes de cobrar; devuelve true si el total mostrado
// es el autoritativo. Evita que el pago mixto se arme sobre un total desactualizado.
async function asegurarCotizacion() {
  clearTimeout(cotizarTimer);
  await refrescarCotizacion();
  return cotizacion != null && cotizacion.firma === firmaOrden();
}

// --- Generar orden --------------------------------------------------
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
  if (!validarCliente(true)) {
    MammaTomatoAlert.warning("Datos incompletos", "Revisa el nombre y el telefono del cliente");
    return;
  }
  if (!paymentMethod.value) {
    MammaTomatoAlert.warning("Datos incompletos", "Selecciona el metodo de pago");
    marcarError(paymentMethod);
    return;
  }
  // El cobro (y el pago mixto) se arman sobre el total autoritativo con descuentos.
  if (!(await asegurarCotizacion())) {
    MammaTomatoAlert.error("No se pudo calcular el total", "Revisa tu conexion e intenta nuevamente");
    return;
  }
  abrirOrdenModal();
}

function metodoPagoActual() {
  const opcion = paymentMethod.options[paymentMethod.selectedIndex];
  return opcion ? opcion.textContent.trim() : "";
}

function esEfectivo() {
  return /efectivo/i.test(metodoPagoActual());
}

function esBilleteraDigital() {
  return /yape|plin/i.test(metodoPagoActual());
}

// --- Tiempo estimado de entrega -------------------------------------
// Minutos de preparacion por unidad segun el tipo de producto. La deteccion
// es por nombre de categoria (sin tildes ni mayusculas).
const MINUTOS_POR_TIPO = [
  { test: /combo/, min: 40 }, // combos: plato completo, lo que mas demora
  { test: /pizza/, min: 20 }, // una pizza
  { test: /panizz/, min: 10 }, // panizzas: a lo mas 10 min
  { test: /bebida|gaseosa|jugo|refresco/, min: 3 }, // bebidas solas: 2-3 min
];
const MIN_POR_ITEM_DEFAULT = 5; // otros productos: por item

function normalizar(texto) {
  return (texto || "").toLowerCase().replace(/[áàä]/g, "a").replace(/[éèë]/g, "e").replace(/[íìï]/g, "i").replace(/[óòö]/g, "o").replace(/[úùü]/g, "u");
}

function minutosPorUnidad(categoria) {
  const cat = normalizar(categoria);
  const regla = MINUTOS_POR_TIPO.find((r) => r.test.test(cat));
  return regla ? regla.min : MIN_POR_ITEM_DEFAULT;
}

// Tiempo de preparacion de un conjunto de items, segun el tipo de cada producto.
function tiempoPreparacion(items) {
  return (items || []).reduce((sum, item) => sum + minutosPorUnidad(item.categoria) * item.cantidad, 0);
}

// Suma de los tiempos de preparacion de los pedidos por delante en la cola
// (pendientes + en preparacion): el cliente espera a que se preparen antes que el suyo.
async function tiempoColaCocina() {
  try {
    const pedidos = await getJson("/api/pedidos/cocina");
    return pedidos
      .filter((p) => ["PENDIENTE", "PREPARANDO"].includes(String(p.estado).toUpperCase()))
      .reduce((sum, p) => sum + tiempoPreparacion(p.items), 0);
  } catch {
    return 0; // si falla, estimamos solo con el pedido actual
  }
}

// Espera del cliente = preparacion de su pedido + suma de la cola por delante.
async function estimarTiempoEspera(items) {
  const propio = tiempoPreparacion(items);
  const cola = await tiempoColaCocina();
  return propio + cola;
}

async function enviarVenta(vuelto, pagos, desglose) {
  const tipo = tipoComprobante ? tipoComprobante.value : "BOLETA";
  const payload = {
    clienteNombre: customerName.value.trim(),
    clienteTelefono: telefonoRaw() || null,
    idMetodoPago: Number(paymentMethod.value),
    items: order.map((item) => ({ idProducto: item.idProducto, cantidad: item.cantidad, observacion: item.observacion || null })),
    pagos: pagos && pagos.length ? pagos : null,
    tipoComprobante: tipo,
    clienteDni: clienteDni && clienteDni.value.trim() ? clienteDni.value.trim() : null,
    clienteRuc: tipo === "FACTURA" && clienteRuc ? clienteRuc.value.trim() : null,
    clienteRazonSocial: tipo === "FACTURA" && clienteRazonSocial ? clienteRazonSocial.value.trim() : null,
    clienteEmail: tipo === "BOLETA_ELECTRONICA" && clienteEmail ? clienteEmail.value.trim() : null,
    mesa: mesaSeleccionada || null,
  };

  // El tiempo se estima ANTES de crear el pedido para que la cola no se cuente a si mismo.
  const tiempoEstimado = await estimarTiempoEspera(order);

  generateTicketBtn.disabled = true;
  try {
    const boleta = await sendJson("POST", "/api/pedidos", payload);
    showOrderConfirm(boleta, tiempoEstimado, vuelto, desglose);
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
  limpiarEstado(customerName);
  limpiarEstado(customerPhone);
  reiniciarComprobante();
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
const confirmPrint = document.querySelector("#confirmPrint");

// Id del pedido de la ultima venta, para imprimir su boleta desde la confirmacion.
let ultimoPedidoBoleta = null;

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
const cobroMixtoLineas = document.querySelector("#cobroMixtoLineas");
const cobroAddMetodo = document.querySelector("#cobroAddMetodo");
const cobroLineaTpl = document.querySelector("#cobroLineaTpl");
const cobroMixtoResumen = document.querySelector("#cobroMixtoResumen");
const cobroEfectivoBlock = document.querySelector("#cobroEfectivoBlock");

function showOrderConfirm(boleta, tiempoEstimado, vuelto, desglose) {
  ultimoPedidoBoleta = boleta.idPedido != null ? boleta.idPedido : null;
  if (confirmPrint) confirmPrint.disabled = ultimoPedidoBoleta == null;
  const numero = boleta.numeroBoleta || `#${boleta.idPedido}`;
  confirmTitle.textContent = `Orden ${numero} enviada a cocina`;
  let info = `<i class="bi bi-clock"></i> Tiempo estimado: ${tiempoEstimado} min.`;
  if (boleta.mesa) {
    info += `<br><i class="bi bi-geo-alt"></i> Ubicacion: ${boleta.mesa}`;
  }
  if (Array.isArray(desglose) && desglose.length) {
    const partes = desglose.map((p) => `${p.metodo} ${money(p.monto)}`).join(" + ");
    info += `<br><i class="bi bi-wallet2"></i> Pago: ${partes}`;
  }
  if (vuelto != null && vuelto > 0.001) {
    info += `<br><i class="bi bi-cash-coin"></i> Vuelto: ${money(vuelto)}`;
  }
  if (boleta.emailEstado === "ENVIADO") {
    info += `<br><i class="bi bi-envelope-check"></i> Comprobante enviado por email`;
  } else if (boleta.emailEstado === "NO_CONFIGURADO" || boleta.emailEstado === "PENDIENTE") {
    info += `<br><i class="bi bi-envelope-exclamation"></i> Envio de email pendiente (SMTP no configurado)`;
  }
  confirmEta.innerHTML = info;
  orderConfirm.classList.remove("hidden");
}

function hideOrderConfirm() {
  orderConfirm.classList.add("hidden");
}

confirmDismiss.addEventListener("click", hideOrderConfirm);
confirmNew.addEventListener("click", hideOrderConfirm);

// Abre la boleta imprimible (ticket con logo + QR) en una ventana aparte.
if (confirmPrint) {
  confirmPrint.addEventListener("click", () => {
    if (ultimoPedidoBoleta == null) return;
    window.open(`/cajero/boleta/${ultimoPedidoBoleta}`, "_blank", "width=420,height=720");
  });
}
orderConfirm.addEventListener("click", (e) => { if (e.target === orderConfirm) hideOrderConfirm(); });

// --- Cobro en efectivo + vuelto -------------------------------------
let cobroTotal = 0;

function abrirCobro() {
  cobroTotal = getTotals().total;
  cobroTotalNode.textContent = money(cobroTotal);
  cobroRecibido.value = "";
  cobroMixto.checked = false;
  cobroMixtoLineas.innerHTML = "";
  cobroMixtoBlock.classList.add("hidden");
  actualizarVuelto();
  cobroModal.classList.remove("hidden");
  setTimeout(() => cobroRecibido.focus(), 50);
}

function cerrarCobro() {
  cobroModal.classList.add("hidden");
}

// Lineas de pago "no efectivo" del pago mixto (cada una metodo + monto).
function partesMixto() {
  return Array.from(cobroMixtoLineas.querySelectorAll(".cobro-mixto-row")).map((row) => {
    const sel = row.querySelector(".cobro-linea-metodo");
    const monto = Number(row.querySelector(".cobro-linea-monto").value) || 0;
    return {
      idMetodoPago: Number(sel.value),
      metodo: metodoLabel(sel),
      monto: Number(monto.toFixed(2)),
      valida: Boolean(sel.value) && monto > 0.001,
    };
  });
}

function agregarLinea() {
  const linea = cobroLineaTpl.content.firstElementChild.cloneNode(true);
  const sel = linea.querySelector(".cobro-linea-metodo");
  const monto = linea.querySelector(".cobro-linea-monto");
  const qrBtn = linea.querySelector(".cobro-linea-qr");

  // El boton de QR aparece solo cuando el metodo de la linea es billetera digital.
  const refrescarQrLinea = () => { if (qrBtn) qrBtn.hidden = !/yape|plin/i.test(metodoLabel(sel)); };

  sel.addEventListener("change", () => { refrescarQrLinea(); actualizarVuelto(); });
  monto.addEventListener("input", actualizarVuelto);
  if (qrBtn) {
    qrBtn.addEventListener("click", () => {
      const importe = Number(monto.value) || 0;
      if (importe <= 0.001) {
        MammaTomatoAlert.warning("Monto requerido", "Ingresa el monto de esta linea para ver su QR");
        return;
      }
      mostrarPagoQr(metodoLabel(sel), importe, "mixto");
    });
  }
  linea.querySelector(".cobro-linea-quitar").addEventListener("click", () => {
    linea.remove();
    actualizarVuelto();
  });
  cobroMixtoLineas.appendChild(linea);
  return linea;
}

// Suma de las partes no-efectivo; el resto se cobra en efectivo.
function montoDigital() {
  return cobroMixto.checked
    ? partesMixto().reduce((suma, p) => suma + p.monto, 0)
    : 0;
}

function porcionEfectivo() {
  return Math.max(0, cobroTotal - montoDigital());
}

function actualizarVuelto() {
  const efectivo = porcionEfectivo();
  const recibido = Number(cobroRecibido.value) || 0;
  const vuelto = recibido - efectivo;
  const falta = recibido > 0 && vuelto < -0.001;
  const exacto = !falta && recibido > 0.001 && efectivo > 0.001 && Math.abs(vuelto) < 0.005;
  cobroVueltoBox.classList.toggle("falta", falta);
  cobroVueltoBox.classList.toggle("exacto", exacto);
  cobroVueltoNode.textContent = falta
    ? `Falta ${money(Math.abs(vuelto))}`
    : exacto ? `${money(0)} · EXACTO`
    : money(Math.max(0, vuelto));

  // Si los otros metodos ya cubren el total, se oculta el cobro en efectivo.
  if (cobroMixto.checked) {
    const otros = montoDigital();
    cobroEfectivoBlock.classList.toggle("hidden", efectivo <= 0.001);
    if (otros > cobroTotal + 0.001) {
      cobroMixtoResumen.textContent = `Los metodos (${money(otros)}) superan el total (${money(cobroTotal)})`;
    } else if (efectivo > 0.001) {
      cobroMixtoResumen.textContent = `Otros metodos: ${money(otros)} · falta en efectivo: ${money(efectivo)}`;
    } else {
      cobroMixtoResumen.textContent = `Cubierto por otros metodos: ${money(otros)} · sin efectivo`;
    }
  } else {
    cobroEfectivoBlock.classList.remove("hidden");
  }

  cobroConfirm.disabled = !cobroPuedeConfirmar(recibido, efectivo);
}

// Confirmable si el efectivo recibido cubre su parte y, en mixto, cada linea es valida
// y las partes no superan el total.
function cobroPuedeConfirmar(recibido, efectivo) {
  if (recibido + 0.001 < efectivo) return false;
  if (!cobroMixto.checked) return true;
  const partes = partesMixto();
  if (!partes.length || partes.some((p) => !p.valida)) return false;
  const suma = partes.reduce((s, p) => s + p.monto, 0);
  return suma > 0.001 && suma <= cobroTotal + 0.001;
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
  if (cobroMixto.checked) {
    if (!cobroMixtoLineas.children.length) agregarLinea();
  } else {
    cobroMixtoLineas.innerHTML = "";
  }
  actualizarVuelto();
});
cobroAddMetodo.addEventListener("click", () => { agregarLinea(); actualizarVuelto(); });

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
    const partes = partesMixto().filter((p) => p.valida);
    pagos = partes.map((p) => ({ idMetodoPago: p.idMetodoPago, monto: p.monto }));
    desglose = partes.map((p) => ({ metodo: p.metodo, monto: p.monto }));
    // El efectivo solo entra como parte si los otros metodos no cubren el total.
    if (efectivo > 0.001) {
      const efe = Number(efectivo.toFixed(2));
      pagos.unshift({ idMetodoPago: Number(paymentMethod.value), monto: efe });
      desglose.unshift({ metodo: metodoLabel(paymentMethod), monto: efe });
    }
  }
  cerrarCobro();
  enviarVenta(efectivo > 0.001 ? vuelto : 0, pagos, desglose);
});

// --- Atajos de teclado ----------------------------------------------
document.addEventListener("keydown", (e) => {
  const ordenAbierto = ordenModal && !ordenModal.classList.contains("hidden");
  const pagoQrAbierto = pagoQrModal && !pagoQrModal.classList.contains("hidden");
  if (pagoQrAbierto) {
    if (e.key === "Escape") { e.preventDefault(); cerrarPagoQr(); }
    return;
  }
  if (ordenAbierto) {
    if (e.key === "Escape") { e.preventDefault(); cerrarOrdenModal(); }
    else if (e.key === "Enter") { e.preventDefault(); completarOrden(); }
    return;
  }
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

// =====================================================================
//  Validacion en tiempo real (estilo Bootstrap) del cliente
// =====================================================================
const RE_NOMBRE = /^[A-Za-zÁÉÍÓÚÑáéíóúñ\s]{2,}$/;

function setEstado(input, ok) {
  if (input) {
    input.classList.toggle("is-valid", ok);
    input.classList.toggle("is-invalid", !ok);
  }
  return ok;
}

function limpiarEstado(input) {
  if (input) input.classList.remove("is-valid", "is-invalid");
}

function validarNombre() {
  return setEstado(customerName, RE_NOMBRE.test(customerName.value.trim()));
}

// Telefono en crudo: solo digitos, maximo 9 (lo que se envia al backend; char(9)).
function telefonoRaw() {
  return customerPhone.value.replace(/\D/g, "").slice(0, 9);
}

// Formato visual agrupado de 3 en 3: "900-000-000".
function formatearTelefono(dig) {
  return [dig.slice(0, 3), dig.slice(3, 6), dig.slice(6, 9)].filter(Boolean).join("-");
}

function validarTelefono() {
  const dig = telefonoRaw();
  if (dig === "") { limpiarEstado(customerPhone); return true; } // el telefono es OPCIONAL
  return setEstado(customerPhone, /^\d{9}$/.test(dig));
}

function validarCliente() {
  const okNombre = validarNombre();
  const okTelefono = validarTelefono();
  return okNombre && okTelefono;
}

customerName.addEventListener("input", validarNombre);
customerPhone.addEventListener("input", () => {
  customerPhone.value = formatearTelefono(telefonoRaw());
  validarTelefono();
});

// =====================================================================
//  Modal de orden: mesas + comprobante + pago
// =====================================================================
const ordenModal = document.querySelector("#ordenModal");
const ordenClose = document.querySelector("#ordenClose");
const mesaPlano = document.querySelector("#mesaPlano");
const mesaSeleccionLabel = document.querySelector("#mesaSeleccionLabel");
const ordenNumero = document.querySelector("#ordenNumero");
const ordenTiempo = document.querySelector("#ordenTiempo");
const ordenTotal = document.querySelector("#ordenTotal");
const tipoComprobante = document.querySelector("#tipoComprobante");
const campoDni = document.querySelector("#campoDni");
const campoFactura = document.querySelector("#campoFactura");
const campoEmail = document.querySelector("#campoEmail");
const clienteDni = document.querySelector("#clienteDni");
const clienteRuc = document.querySelector("#clienteRuc");
const clienteRazonSocial = document.querySelector("#clienteRazonSocial");
const clienteEmail = document.querySelector("#clienteEmail");
const guardarRucBtn = document.querySelector("#guardarRucBtn");
const rucEstado = document.querySelector("#rucEstado");
const completarOrdenBtn = document.querySelector("#completarOrdenBtn");

const pagoQrModal = document.querySelector("#pagoQrModal");
const pagoQrClose = document.querySelector("#pagoQrClose");
const pagoQrImg = document.querySelector("#pagoQrImg");
const pagoQrMonto = document.querySelector("#pagoQrMonto");
const pagoQrTitle = document.querySelector("#pagoQrTitle");
const pagoQrConfirm = document.querySelector("#pagoQrConfirm");

let mesaSeleccionada = ""; // "" = para llevar / sin mesa
let mesasRenderizadas = false;

// El local tiene 4 mesas de 4 asientos; se dibujan con un SVG limpio e identico.
const MESA_SVG = `
  <svg class="mesa-svg" viewBox="0 0 100 100" aria-hidden="true">
    <rect class="asiento" x="38" y="5"  width="24" height="13" rx="6"/>
    <rect class="asiento" x="38" y="82" width="24" height="13" rx="6"/>
    <rect class="asiento" x="5"  y="38" width="13" height="24" rx="6"/>
    <rect class="asiento" x="82" y="38" width="13" height="24" rx="6"/>
    <rect class="tabla" x="25" y="25" width="50" height="50" rx="12"/>
  </svg>`;

function renderMesas() {
  if (mesasRenderizadas || !mesaPlano) return;
  for (let i = 1; i <= 4; i++) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = "mesa-btn";
    btn.dataset.mesa = `MESA-${i}`;
    btn.innerHTML = `${MESA_SVG}<span class="mesa-label">Mesa ${i}</span><span class="mesa-sub">4 asientos</span>`;
    btn.addEventListener("click", () => seleccionarMesa(`MESA-${i}`, `Mesa ${i}`));
    mesaPlano.append(btn);
  }
  mesasRenderizadas = true;
}

function seleccionarMesa(valor, etiqueta) {
  mesaSeleccionada = valor;
  document.querySelectorAll(".mesa-btn, .mesa-chip").forEach((el) => {
    el.classList.toggle("selected", (el.dataset.mesa || "") === valor);
  });
  if (mesaSeleccionLabel) mesaSeleccionLabel.textContent = etiqueta;
}

document.querySelectorAll(".mesa-chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    const valor = chip.dataset.mesa || "";
    seleccionarMesa(valor, valor === "BARRA" ? "Barra (recojo)" : "Para llevar");
  });
});

function actualizarCamposComprobante() {
  const tipo = tipoComprobante.value;
  campoDni.classList.toggle("hidden", tipo !== "BOLETA");
  campoFactura.classList.toggle("hidden", tipo !== "FACTURA");
  campoEmail.classList.toggle("hidden", tipo !== "BOLETA_ELECTRONICA");
  cargarNumeroOrden();
}

function reiniciarComprobante() {
  if (tipoComprobante) tipoComprobante.value = "BOLETA";
  [clienteDni, clienteRuc, clienteRazonSocial, clienteEmail].forEach((el) => {
    if (el) { el.value = ""; limpiarEstado(el); }
  });
  if (rucEstado) rucEstado.textContent = "";
  if (guardarRucBtn) guardarRucBtn.hidden = true;
  seleccionarMesa("", "Para llevar");
  if (campoDni) actualizarCamposComprobante();
}

function validarComprobante() {
  const tipo = tipoComprobante.value;
  if (tipo === "BOLETA") {
    const v = clienteDni.value.trim();
    if (v === "") { limpiarEstado(clienteDni); return true; } // DNI opcional
    return setEstado(clienteDni, /^\d{8}$/.test(v));
  }
  if (tipo === "FACTURA") {
    const okRuc = setEstado(clienteRuc, /^\d{11}$/.test(clienteRuc.value.trim()));
    const okRazon = setEstado(clienteRazonSocial, clienteRazonSocial.value.trim().length >= 2);
    return okRuc && okRazon;
  }
  if (tipo === "BOLETA_ELECTRONICA") {
    return setEstado(clienteEmail, /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(clienteEmail.value.trim()));
  }
  return true;
}

// Validacion en tiempo real de los campos del comprobante
clienteDni.addEventListener("input", () => {
  clienteDni.value = clienteDni.value.replace(/\D/g, "").slice(0, 8);
  if (clienteDni.value === "") limpiarEstado(clienteDni);
  else setEstado(clienteDni, /^\d{8}$/.test(clienteDni.value));
});
clienteRazonSocial.addEventListener("input", () =>
  setEstado(clienteRazonSocial, clienteRazonSocial.value.trim().length >= 2));
clienteEmail.addEventListener("input", () =>
  setEstado(clienteEmail, /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(clienteEmail.value.trim())));

// RUC: validacion + autocompletado de razon social desde la BD
let rucTimer = null;
clienteRuc.addEventListener("input", () => {
  clienteRuc.value = clienteRuc.value.replace(/\D/g, "").slice(0, 11);
  setEstado(clienteRuc, /^\d{11}$/.test(clienteRuc.value));
  rucEstado.textContent = "";
  guardarRucBtn.hidden = true;
  clearTimeout(rucTimer);
  if (/^\d{11}$/.test(clienteRuc.value)) rucTimer = setTimeout(buscarRuc, 400);
});

async function buscarRuc() {
  const ruc = clienteRuc.value.trim();
  try {
    const data = await getJson(`/cajero/clientes-empresa/${ruc}`);
    if (data.encontrado) {
      clienteRazonSocial.value = data.razonSocial;
      setEstado(clienteRazonSocial, true);
      rucEstado.className = "ruc-estado";
      rucEstado.textContent = "RUC registrado: razon social autocompletada.";
      guardarRucBtn.hidden = true;
    } else {
      rucEstado.className = "ruc-estado pendiente";
      rucEstado.textContent = "RUC no registrado. Completa la razon social y guardalo.";
      guardarRucBtn.hidden = false;
    }
  } catch {
    /* silencioso: el autocompletado es de apoyo */
  }
}

guardarRucBtn.addEventListener("click", async () => {
  const ruc = clienteRuc.value.trim();
  const razon = clienteRazonSocial.value.trim();
  if (!/^\d{11}$/.test(ruc) || razon.length < 2) {
    MammaTomatoAlert.warning("Datos incompletos", "Ingresa RUC (11 digitos) y razon social");
    return;
  }
  try {
    await sendJson("POST", "/cajero/clientes-empresa", { ruc, razonSocial: razon });
    rucEstado.className = "ruc-estado";
    rucEstado.textContent = "RUC guardado correctamente.";
    guardarRucBtn.hidden = true;
  } catch (err) {
    MammaTomatoAlert.error("No se pudo guardar el RUC", err.message);
  }
});

tipoComprobante.addEventListener("change", actualizarCamposComprobante);

async function cargarNumeroOrden() {
  try {
    const data = await getJson(`/cajero/comprobante/siguiente?tipo=${encodeURIComponent(tipoComprobante.value)}`);
    ordenNumero.textContent = data.numero;
  } catch {
    ordenNumero.textContent = "—";
  }
}

function abrirOrdenModal() {
  renderMesas();
  ordenTotal.textContent = money(getTotals().total);
  actualizarCamposComprobante();
  ordenTiempo.textContent = "… min";
  ordenModal.classList.remove("hidden");
  estimarTiempoEspera(order).then((min) => { ordenTiempo.textContent = `${min} min`; });
}

function cerrarOrdenModal() {
  ordenModal.classList.add("hidden");
}

function completarOrden() {
  if (!validarComprobante()) {
    MammaTomatoAlert.warning("Comprobante incompleto", "Revisa los datos del comprobante");
    return;
  }
  cerrarOrdenModal();
  if (esEfectivo()) abrirCobro();
  else if (esBilleteraDigital()) abrirPagoQr();
  else enviarVenta(null, null);
}

completarOrdenBtn.addEventListener("click", completarOrden);
ordenClose.addEventListener("click", cerrarOrdenModal);
ordenModal.addEventListener("click", (e) => { if (e.target === ordenModal) cerrarOrdenModal(); });

// --- Pago con QR (Yape / Plin) ---
// modo "principal": el metodo de pago de la venta es billetera -> al confirmar se genera la venta.
// modo "mixto": es una linea del pago mixto -> el QR es informativo y el cobro sigue en #cobroModal.
let pagoQrModo = "principal";

function mostrarPagoQr(metodo, monto, modo) {
  pagoQrModo = modo || "principal";
  pagoQrMonto.textContent = money(monto);
  pagoQrTitle.textContent = `Pago con ${metodo}`;
  pagoQrImg.src = `/cajero/pago/qr?metodo=${encodeURIComponent(metodo)}&monto=${Number(monto).toFixed(2)}`;
  pagoQrConfirm.innerHTML = pagoQrModo === "mixto"
    ? '<i class="bi bi-check2"></i> Listo'
    : '<i class="bi bi-check2"></i> Confirmar pago y generar';
  pagoQrModal.classList.remove("hidden");
}

function abrirPagoQr() {
  mostrarPagoQr(metodoPagoActual(), getTotals().total, "principal");
}

function cerrarPagoQr() {
  pagoQrModal.classList.add("hidden");
}

pagoQrClose.addEventListener("click", cerrarPagoQr);
pagoQrModal.addEventListener("click", (e) => { if (e.target === pagoQrModal) cerrarPagoQr(); });
pagoQrConfirm.addEventListener("click", () => {
  cerrarPagoQr();
  // En modo mixto el cobro continua en el modal de efectivo; no se envia aqui.
  if (pagoQrModo === "principal") enviarVenta(null, null);
});

setupCategories();
loadProducts();
cargarPedidoRecuperado();
renderOrder();
actualizarBadgeGuardados();

// Tiempo real: la cocina avisa al cajero cuando mueve un pedido.
if (window.MammaTomatoRealtime && window.MammaTomatoAlert) {
  window.MammaTomatoRealtime.on("pedido-estado", (d) => {
    const cod = `PED-${String(d.idPedido).padStart(3, "0")}`;
    if (d.estado === "ATENDIDO") {
      window.MammaTomatoAlert.success("Pedido entregado", `Cocina marco ${cod} como entregado`);
    } else if (d.estado === "PREPARANDO") {
      window.MammaTomatoAlert.info("En preparacion", `Cocina empezo a preparar ${cod}`);
    }
  });
}
