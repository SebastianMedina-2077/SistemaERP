import { setupUserBox } from "./app.js";

setupUserBox("Administrador");

const resolvePageUrl = (page) => new URL(`../pages/${page}.html`, import.meta.url);

const content = document.querySelector("#content");
const title = document.querySelector("#viewTitle");
const navItems = Array.from(document.querySelectorAll(".nav-item"));
const areaSelect = document.querySelector("#moduleAreaSelect");

const moduleLoaders = {
  dashboard: () => import("./modules/dashboard.js"),
  ventas: () => import("./modules/ventas.js"),
  catalogo: () => import("./modules/catalogo.js"),
  inventario: () => import("./modules/inventario.js"),
  personas: () => import("./modules/personas.js"),
  reportes: () => import("./modules/reportes.js")
};

const titles = {
  dashboard: "Dashboard",
  pedidos: "Pedidos",
  productos: "Productos",
  promociones: "Promociones",
  insumos: "Insumos",
  compras: "Compras",
  movimientos: "Movimientos",
  kardex: "Kardex",
  empleados: "Empleados",
  usuarios: "Usuarios",
  proveedores: "Proveedores",
  "reportes:ventas": "Reportes de ventas",
  "reportes:compras": "Reportes de compras",
  "reportes:productos": "Productos mas vendidos",
  "reportes:inventario": "Movimientos de inventario",
  "reportes:anulados": "Pedidos anulados"
};

function isVisibleForArea(button, area) {
  return button.dataset.area === "general" || button.dataset.area === area;
}

function setActiveNav(button) {
  navItems.forEach((item) => item.classList.remove("active"));
  button.classList.add("active");
}

function applyArea(area, { loadFirst = true } = {}) {
  navItems.forEach((button) => {
    button.hidden = !isVisibleForArea(button, area);
  });

  if (!loadFirst) return;

  const firstAreaItem = navItems.find((button) => button.dataset.area === area);
  if (!firstAreaItem) return;
  setActiveNav(firstAreaItem);
  loadPage(firstAreaItem.dataset.page, firstAreaItem.dataset.section);
}

async function loadPage(page, section) {
  const response = await fetch(resolvePageUrl(page));
  if (!response.ok) {
    content.innerHTML = `<section class="admin-section active"><div class="card panel">No se pudo cargar la pagina ${page}.html</div></section>`;
    title.textContent = "Error de carga";
    return;
  }
  content.innerHTML = await response.text();

  content.querySelectorAll(".admin-section").forEach((node) => {
    node.classList.toggle("active", node.id === section);
  });

  const module = await moduleLoaders[page]();
  module.init(content, section);
  title.textContent = titles[`${page}:${section}`] || titles[section] || titles[page] || "ERP";
  content.focus();
}

navItems.forEach((button) => {
  button.addEventListener("click", () => {
    setActiveNav(button);
    loadPage(button.dataset.page, button.dataset.section);
  });
});

areaSelect.addEventListener("change", () => {
  applyArea(areaSelect.value);
});

applyArea(areaSelect.value, { loadFirst: false });
loadPage("dashboard", "dashboard");
