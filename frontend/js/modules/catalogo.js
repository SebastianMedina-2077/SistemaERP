import { actionButtons, appendRows, badge, cell, clear, el, fillSelect, row } from "../app.js";
import { CatalogService, format, InventoryService } from "../services.js";

export function init(root) {
  renderCategoryOptions(root);
  renderProducts(root);
  renderRecipes(root);
  renderPromotions(root);
  renderCombos(root);
}

function renderCategoryOptions(root) {
  const select = root.querySelector("#productCategoryOptions");
  if (select) fillSelect(select, CatalogService.getCategorias(), (item) => item.idCategoria, (item) => item.nombre);
}

function renderProducts(root) {
  const target = root.querySelector("#productsTable");
  if (!target) return;
  appendRows(target, CatalogService.getProductos().map((item) => {
    const category = CatalogService.getCategoria(item.idCategoria);
    const status = item.disponible ? "Disponible" : "No disponible";
    return row(
      cell(item.codigo),
      cell(item.nombre),
      cell(category.nombre),
      cell(item.tamanio || "-"),
      cell(format.money(item.precio)),
      cell(item.stock === null ? "NULL" : item.stock),
      cell(badge(status, format.badge(status))),
      cell(actionButtons())
    );
  }));
}

function renderRecipes(root) {
  const target = root.querySelector("#recipesList");
  if (!target) return;
  clear(target);
  target.append(...CatalogService.getRecetas().map((recipe) => {
    const list = el("ul", { className: "detail-list" });
    list.append(...recipe.items.map((item) => {
      const measure = InventoryService.getMedida(item.insumo.idMedida);
      return el("li", {}, `${item.insumo.nombre}: `, el("strong", { text: `${item.cantidad} ${measure.sigla}` }));
    }));
    return el("article", { className: "card panel" },
      el("div", { className: "section-heading" },
        el("h2", { text: recipe.producto.nombre }),
        badge(recipe.producto.codigo, "badge badge-info")
      ),
      list
    );
  }));
}

function renderPromotions(root) {
  const target = root.querySelector("#promotionsTable");
  if (!target) return;
  appendRows(target, CatalogService.getPromociones().map((item) => {
    const status = item.activa ? "Activa" : "Inactiva";
    return row(
      cell(item.nombre),
      cell(item.descripcion),
      cell(item.tipoDescuento),
      cell(item.tipoDescuento === "Porcentaje" ? `${item.valorDescuento}%` : format.money(item.valorDescuento)),
      cell(badge(status, format.badge(status))),
      cell(actionButtons())
    );
  }));
}

function renderCombos(root) {
  const target = root.querySelector("#combosList");
  if (!target) return;
  clear(target);
  target.append(...CatalogService.getCombos().map((combo) => {
    const list = el("ul", { className: "detail-list" });
    list.append(...combo.items.map((item) => el("li", {}, `${item.producto.nombre}: `, el("strong", { text: `x${item.cantidad}` }))));
    return el("article", { className: "card panel" },
      el("div", { className: "section-heading" },
        el("h2", { text: combo.combo.nombre }),
        badge(format.money(combo.combo.precio), "badge badge-success")
      ),
      list
    );
  }));
}
