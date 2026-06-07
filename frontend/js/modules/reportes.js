import { appendRows, badge, cell, clear, el, row } from "../app.js";
import { format, InventoryService, PeopleService, ReportService, SalesService } from "../services.js";

export function init(root) {
  renderSalesStats(root);
  renderPurchases(root);
  renderTopProducts(root);
  renderInventory(root);
  renderCancelled(root);
}

function renderSalesStats(root) {
  const target = root.querySelector("#salesReportStats");
  if (!target) return;
  clear(target);
  target.append(...ReportService.getSalesStats().map((item) => el("article", { className: "card stat-card" },
    el("span", { text: item.label }),
    el("strong", { text: item.value })
  )));
}

function renderPurchases(root) {
  const target = root.querySelector("#purchaseReportTable");
  if (!target) return;
  const rows = PeopleService.getProveedores().map((supplier) => {
    const purchases = InventoryService.getCompras().filter((item) => item.idProveedor === supplier.idProveedor);
    const total = purchases.reduce((sum, item) => sum + item.total, 0);
    return { supplier, count: purchases.length, total, last: purchases[0]?.fecha || "-" };
  });
  appendRows(target, rows.map((item) => row(
    cell(item.supplier.nombre),
    cell(item.count),
    cell(format.money(item.total)),
    cell(item.last)
  )));
}

function renderTopProducts(root) {
  const target = root.querySelector("#bestProductsReport");
  if (!target) return;
  const top = SalesService.getTopProducts();
  const max = Math.max(...top.map((item) => item.cantidad), 1);
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

function renderInventory(root) {
  const target = root.querySelector("#inventoryReportTable");
  if (!target) return;
  const groups = InventoryService.getMovimientos().reduce((acc, item) => {
    const key = item.tipo.descripcion;
    if (!acc[key]) acc[key] = { tipo: item.tipo.descripcion, operacion: item.tipo.operacion, total: 0, usuario: item.usuario.empleado.nombre };
    acc[key].total += 1;
    return acc;
  }, {});
  appendRows(target, Object.values(groups).map((item) => row(
    cell(item.tipo),
    cell(badge(item.operacion, format.badge(item.operacion))),
    cell(item.total),
    cell(item.usuario)
  )));
}

function renderCancelled(root) {
  const target = root.querySelector("#cancelledOrdersTable");
  if (!target) return;
  appendRows(target, SalesService.getPedidos().filter((item) => item.estado === "Anulado").map((item) => row(
    cell(`#${item.idPedido}`),
    cell(item.fecha),
    cell(item.cliente.nombre),
    cell(item.usuario.empleado.nombre),
    cell(format.money(item.boleta?.total || 0)),
    cell(item.motivoAnulacion || "No especificado")
  )));
}
