import { appendRows, badge, cell, clear, el, row } from "../app.js";
import { format, InventoryService, ReportService, SalesService } from "../services.js";

export function init(root) {
  renderStats(root);
  renderTopProducts(root);
  renderLowStock(root);
  renderPurchases(root);
  renderMovements(root);
}

function renderStats(root) {
  const target = root.querySelector("#dashboardStats");
  clear(target);
  target.append(...ReportService.getDashboardStats().map((item) => (
    el("article", { className: "card stat-card" },
      el("span", { text: item.label }),
      el("strong", { text: item.value })
    )
  )));
}

function renderTopProducts(root) {
  const top = SalesService.getTopProducts();
  const max = Math.max(...top.map((item) => item.cantidad), 1);
  const target = root.querySelector("#topProductsChart");
  clear(target);
  target.append(...top.map((item) => {
    const fill = el("div", { className: "bar-fill" });
    fill.style.width = `${(item.cantidad / max) * 100}%`;
    return el("div", { className: "bar-row" },
      el("span", { text: item.producto.nombre }),
      el("div", { className: "bar-track" }, fill),
      el("strong", { text: item.cantidad })
    );
  }));
}

function renderLowStock(root) {
  const items = InventoryService.getLowStock();
  const target = root.querySelector("#lowStockAlerts");
  clear(target);
  if (!items.length) {
    target.append(el("div", { className: "empty-state", text: "No hay insumos bajo stock." }));
    return;
  }
  target.append(...items.map((item) => {
    const medida = InventoryService.getMedida(item.idMedida);
    return el("div", { className: "alert-item" },
      el("div", {},
        el("strong", { text: item.nombre }),
        el("p", { className: "muted", text: `${item.stock} ${medida.sigla} disponibles` })
      ),
      badge(`Min ${item.stockMinimo}`, "badge badge-warning")
    );
  }));
}

function renderPurchases(root) {
  appendRows(root.querySelector("#recentPurchases"), InventoryService.getCompras().map((item) => row(
    cell(item.fecha),
    cell(item.proveedor.nombre),
    cell(badge(item.estado, format.badge(item.estado))),
    cell(format.money(item.total))
  )));
}

function renderMovements(root) {
  appendRows(root.querySelector("#recentMovements"), InventoryService.getMovimientos().map((item) => row(
    cell(item.fecha),
    cell(badge(item.tipo.descripcion, format.badge(item.tipo.operacion))),
    cell(item.documento),
    cell(item.usuario.empleado.nombre)
  )));
}
