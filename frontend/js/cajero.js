import { clear, el, setupUserBox, textButton } from "./app.js";
import { CatalogService, format, InventoryService, SalesService } from "./services.js";

setupUserBox("Cajero");

const categoryButtons = document.querySelector("#categoryButtons");
const productGrid = document.querySelector("#productGrid");
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

let activeCategory = CatalogService.getCategorias()[0].idCategoria;
let order = [];

function renderCategories() {
  clear(categoryButtons);
  categoryButtons.append(...CatalogService.getCategorias().map((category) => {
    const className = `btn ${category.idCategoria === activeCategory ? "btn-secondary" : "btn-ghost"} category-btn`;
    return textButton(category.nombre, className, { category: category.idCategoria });
  }));

  categoryButtons.querySelectorAll("button").forEach((button) => {
    button.addEventListener("click", () => {
      activeCategory = Number(button.dataset.category);
      renderCategories();
      renderProducts();
    });
  });
}

function renderProducts() {
  const products = CatalogService.getProductosDisponibles().filter((product) => product.idCategoria === activeCategory);
  clear(productGrid);
  productGrid.append(...products.map((product) => {
    const stockText = product.stock === null ? "Preparado" : `Stock ${product.stock}`;
    return el("button", { className: "product-btn", type: "button", dataset: { product: product.idProducto } },
      el("strong", { text: product.nombre }),
      el("span", { text: `${format.money(product.precio)} · ${stockText}` })
    );
  }));

  productGrid.querySelectorAll("button").forEach((button) => {
    button.addEventListener("click", () => addProduct(Number(button.dataset.product)));
  });
}

function addProduct(idProducto) {
  const product = CatalogService.getProducto(idProducto);
  const existing = order.find((item) => item.product.idProducto === idProducto);
  const nextQuantity = existing ? existing.quantity + 1 : 1;
  const stockCheck = InventoryService.checkProductStock(product, nextQuantity);

  if (!stockCheck.ok) {
    stockAlert.classList.remove("hidden");
    stockAlert.textContent = `Stock insuficiente: ${stockCheck.missing.map((item) => item.supply.nombre).join(", ")}.`;
    return;
  }

  stockAlert.classList.add("hidden");
  if (existing) existing.quantity += 1;
  else order.push({ product, quantity: 1, observation: "" });
  renderOrder();
}

function renderOrder() {
  if (!order.length) {
    orderItems.className = "order-items empty-state";
    orderItems.textContent = "No hay productos agregados.";
  } else {
    orderItems.className = "order-items";
    clear(orderItems);
    orderItems.append(...order.map((item, index) => {
      const discount = CatalogService.getDiscount(item.product, item.quantity);
      const subtotal = item.product.precio * item.quantity - discount;
      const observation = el("input", { value: item.observation, placeholder: "Ej. sin cebolla", dataset: { action: "obs", index } });
      return el("div", { className: "order-row" },
        el("div", { className: "order-row-head" },
          el("strong", { text: item.product.nombre }),
          el("span", { text: format.money(subtotal) })
        ),
        el("div", { className: "qty-controls" },
          el("button", { text: "-", type: "button", dataset: { action: "dec", index } }),
          el("strong", { text: item.quantity }),
          el("button", { text: "+", type: "button", dataset: { action: "inc", index } }),
          el("span", { className: "muted", text: discount ? `Desc. ${format.money(discount)}` : "Sin descuento" })
        ),
        el("label", {}, "Observacion", observation)
      );
    }));

    orderItems.querySelectorAll("button").forEach((button) => {
      button.addEventListener("click", () => updateQuantity(Number(button.dataset.index), button.dataset.action));
    });
    orderItems.querySelectorAll("input").forEach((input) => {
      input.addEventListener("input", () => {
        order[Number(input.dataset.index)].observation = input.value;
      });
    });
  }
  renderTotals();
}

function updateQuantity(index, action) {
  if (action === "dec") order[index].quantity -= 1;
  if (action === "inc") {
    const item = order[index];
    const stockCheck = InventoryService.checkProductStock(item.product, item.quantity + 1);
    if (!stockCheck.ok) {
      stockAlert.classList.remove("hidden");
      return;
    }
    item.quantity += 1;
  }
  order = order.filter((item) => item.quantity > 0);
  renderOrder();
}

function getTotals() {
  const subtotal = order.reduce((sum, item) => {
    const discount = CatalogService.getDiscount(item.product, item.quantity);
    return sum + item.product.precio * item.quantity - discount;
  }, 0);
  const igv = subtotal * 0.18;
  return { subtotal, igv, total: subtotal + igv };
}

function renderTotals() {
  const totals = getTotals();
  subtotalNode.textContent = format.money(totals.subtotal);
  igvNode.textContent = format.money(totals.igv);
  totalNode.textContent = format.money(totals.total);
}

function generateTicket() {
  if (!customerName.value.trim()) {
    customerName.focus();
    return;
  }
  if (!order.length) return;

  const items = order.map((item) => {
    const discount = CatalogService.getDiscount(item.product, item.quantity);
    return {
      producto: item.product.nombre,
      cantidad: item.quantity,
      precioUnitario: item.product.precio,
      descuento: discount,
      subtotal: item.product.precio * item.quantity - discount,
      observacion: item.observation
    };
  });

  const draft = SalesService.createDraftOrder({
    customerName: customerName.value.trim(),
    customerPhone: customerPhone.value.trim(),
    paymentMethod: paymentMethod.value,
    items
  });

  alert(`Boleta generada: ${draft.numeroBoleta}\nTotal: ${format.money(draft.total)}\nPedido enviado a cocina.`);
  clearOrder();
}

function clearOrder() {
  order = [];
  stockAlert.classList.add("hidden");
  renderOrder();
}

clearOrderBtn.addEventListener("click", clearOrder);
generateTicketBtn.addEventListener("click", generateTicket);

renderCategories();
renderProducts();
renderOrder();
