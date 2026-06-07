import { actionButtons, appendRows, badge, cell, fillSelect, row } from "../app.js";
import { format, InventoryService, PeopleService } from "../services.js";

export function init(root) {
  renderOptionsLists(root);
  renderSupplies(root);
  renderPurchases(root);
  renderMovements(root);
  renderKardex(root);
}

function renderOptionsLists(root) {
  const measures = root.querySelector("#measureOptions");
  const suppliers = root.querySelector("#supplierOptions");
  const kardex = root.querySelector("#kardexSupplyOptions");
  if (measures) fillSelect(measures, InventoryService.getMedidas(), (item) => item.idMedida, (item) => `${item.descripcion} (${item.sigla})`);
  if (suppliers) fillSelect(suppliers, PeopleService.getProveedores(), (item) => item.idProveedor, (item) => item.nombre);
  if (kardex) fillSelect(kardex, InventoryService.getInsumos(), (item) => item.idInsumo, (item) => item.nombre, "Todos");
}

function renderSupplies(root) {
  const target = root.querySelector("#suppliesTable");
  if (!target) return;
  appendRows(target, InventoryService.getInsumos().map((item) => {
    const measure = InventoryService.getMedida(item.idMedida);
    const low = item.stock <= item.stockMinimo;
    const status = low ? "Bajo stock" : "Disponible";
    return row(
      cell(item.codigo),
      cell(item.nombre),
      cell(measure.sigla),
      cell(format.money(item.precio)),
      cell(item.stock),
      cell(item.stockMinimo),
      cell(badge(status, format.badge(status))),
      cell(actionButtons())
    );
  }));
}

function renderPurchases(root) {
  const target = root.querySelector("#purchasesTable");
  if (!target) return;
  appendRows(target, InventoryService.getCompras().map((item) => row(
    cell(item.idCompra),
    cell(item.fecha),
    cell(item.proveedor.nombre),
    cell(item.usuario.empleado.nombre),
    cell(badge(item.estado, format.badge(item.estado))),
    cell(format.money(item.total)),
    cell(actionButtons())
  )));
}

function renderMovements(root) {
  const target = root.querySelector("#movementsTable");
  if (!target) return;
  appendRows(target, InventoryService.getMovimientos().map((item) => row(
    cell(item.idMovimiento),
    cell(item.fecha),
    cell(item.tipo.descripcion),
    cell(badge(item.tipo.operacion, format.badge(item.tipo.operacion))),
    cell(item.documento),
    cell(item.glosa),
    cell(item.usuario.empleado.nombre)
  )));
}

function renderKardex(root) {
  const target = root.querySelector("#kardexTable");
  if (!target) return;
  appendRows(target, InventoryService.getKardex().map((item) => row(
    cell(item.movimiento.fecha),
    cell(item.insumo.nombre),
    cell(item.movimiento.documento),
    cell(item.tipo.descripcion),
    cell(item.entrada || "-"),
    cell(item.salida || "-"),
    cell(item.stockResultante)
  )));
}
