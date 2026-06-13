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
  button.innerHTML = `<strong></strong><span></span>`;
  button.querySelector("strong").textContent = product.nombre;
  button.querySelector("span").textContent = `${money(product.precio)} · ${stockText}`;
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
async function generateTicket() {
  if (!customerName.value.trim()) { customerName.focus(); return; }
  if (!order.length) return;

  const payload = {
    clienteNombre: customerName.value.trim(),
    clienteTelefono: customerPhone.value.trim() || null,
    idMetodoPago: Number(paymentMethod.value),
    items: order.map((item) => ({ idProducto: item.idProducto, cantidad: item.cantidad, observacion: item.observacion || null })),
  };

  const totalItems = order.reduce((sum, item) => sum + item.cantidad, 0);
  const eta = 10 + totalItems * 2;

  generateTicketBtn.disabled = true;
  try {
    const boleta = await sendJson("POST", "/api/pedidos", payload);
    showOrderConfirm(boleta, eta);
    clearOrder();
  } catch (err) {
    const faltantes = err.payload?.detalle;
    if (Array.isArray(faltantes) && faltantes.length && faltantes[0].insumo) {
      showStockAlert(faltantes);
    } else {
      alert(`No se pudo generar la boleta: ${err.message}`);
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

// --- Confirmacion de orden -----------------------------------------
const orderConfirm = document.querySelector("#orderConfirm");
const confirmTitle = document.querySelector("#confirmTitle");
const confirmEta = document.querySelector("#confirmEta");
const confirmDismiss = document.querySelector("#confirmDismiss");
const confirmNew = document.querySelector("#confirmNew");

function showOrderConfirm(boleta, eta) {
  const numero = boleta.idPedido != null ? boleta.idPedido : boleta.numeroBoleta;
  confirmTitle.textContent = `Orden #${numero} enviada a cocina`;
  confirmEta.innerHTML = `<i class="bi bi-clock"></i> Tiempo estimado: ${eta} min.`;
  orderConfirm.classList.remove("hidden");
}

function hideOrderConfirm() {
  orderConfirm.classList.add("hidden");
}

confirmDismiss.addEventListener("click", hideOrderConfirm);
confirmNew.addEventListener("click", hideOrderConfirm);
orderConfirm.addEventListener("click", (e) => { if (e.target === orderConfirm) hideOrderConfirm(); });

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

setupCategories();
loadProducts();
renderOrder();
