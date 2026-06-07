import { actionButtons, appendRows, badge, cell, clear, el, row } from "../app.js";
import { format, SalesService } from "../services.js";

export function init(root, section) {
  renderOrders(root);
  if (section === "pedidos") bindOrderActions(root);
}

function renderOrders(root) {
  const target = root.querySelector("#ordersTable");
  if (!target) return;
  appendRows(target, SalesService.getPedidos().map((pedido) => {
    const tr = row(
      cell(`#${pedido.idPedido}`),
      cell(pedido.fecha),
      cell(pedido.cliente.nombre),
      cell(pedido.cliente.telefono || "-"),
      cell(pedido.usuario.empleado.nombre),
      cell(badge(pedido.estado, format.badge(pedido.estado))),
      cell(format.money(pedido.boleta?.subtotal || 0)),
      cell(format.money(pedido.boleta?.total || 0)),
      cell(actionButtons({
        includeDetalle: true,
        includeBoleta: Boolean(pedido.boleta) && pedido.estado !== "Anulado",
        includeAnular: pedido.estado !== "Anulado"
      }))
    );
    tr.dataset.order = pedido.idPedido;
    return tr;
  }));
}

function bindOrderActions(root) {
  const detail = root.querySelector("#orderDetail");
  root.querySelectorAll(".js-detail").forEach((button) => {
    button.addEventListener("click", () => {
      const idPedido = Number(button.closest("tr").dataset.order);
      const pedido = SalesService.getPedidos().find((item) => item.idPedido === idPedido);
      detail.classList.remove("hidden");
      clear(detail);
      const list = el("ul", { className: "detail-list" });
      list.append(...pedido.detalles.map((item) => {
        const li = el("li", {},
          el("strong", { text: item.producto.nombre }),
          ` x ${item.cantidad} - ${format.money(item.subtotal)}`
        );
        if (item.observacion) li.append(" ", el("span", { className: "muted", text: `(${item.observacion})` }));
        return li;
      }));
      detail.append(
        el("div", { className: "section-heading" },
          el("h2", { text: `Detalle pedido #${pedido.idPedido}` }),
          badge(pedido.estado, format.badge(pedido.estado))
        ),
        list,
        el("p", { className: "muted", text: `Descuentos por producto incluidos. IGV fijo 18%. Metodo: ${pedido.boleta ? pedido.boleta.metodo.descripcion : "-"}` })
      );
    });
  });

  root.querySelectorAll(".js-ticket").forEach((button) => {
    button.addEventListener("click", () => {
      const idPedido = Number(button.closest("tr").dataset.order);
      const pedido = SalesService.getPedidos().find((item) => item.idPedido === idPedido);
      detail.classList.remove("hidden");
      clear(detail);
      const list = el("ul", { className: "detail-list" });
      list.append(...pedido.detalles.map((item) => el("li", {},
        el("strong", { text: item.producto.nombre }),
        ` x ${item.cantidad} - ${format.money(item.subtotal)}`
      )));
      detail.append(
        el("div", { className: "section-heading" },
          el("h2", { text: `Boleta ${pedido.boleta.numeroBoleta}` }),
          badge(pedido.boleta.metodo.descripcion, "badge badge-info")
        ),
        el("p", { className: "muted", text: `Pedido #${pedido.idPedido} - Cliente: ${pedido.cliente.nombre}` }),
        list,
        el("div", { className: "stat-grid" },
          el("article", { className: "card stat-card" }, el("span", { text: "Subtotal" }), el("strong", { text: format.money(pedido.boleta.subtotal) })),
          el("article", { className: "card stat-card" }, el("span", { text: "IGV" }), el("strong", { text: format.money(pedido.boleta.igv) })),
          el("article", { className: "card stat-card" }, el("span", { text: "Total" }), el("strong", { text: format.money(pedido.boleta.total) }))
        )
      );
    });
  });
}
