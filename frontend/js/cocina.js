import { badge, clear, el, setupUserBox } from "./app.js";
import { format, SalesService } from "./services.js";

setupUserBox("Cocina");

const kitchenOrders = document.querySelector("#kitchenOrders");

function renderKitchen() {
  const orders = SalesService.getKitchenOrders();
  clear(kitchenOrders);
  kitchenOrders.append(...orders.map((pedido) => {
    const list = el("ul", { className: "kitchen-items" });
    list.append(...pedido.detalles.map((item) => {
      const li = el("li", {}, el("strong", { text: item.producto.nombre }), ` x ${item.cantidad}`);
      if (item.observacion) li.append(el("p", { className: "muted", text: item.observacion }));
      return li;
    }));
    return el("article", { className: "kitchen-card" },
      el("div", { className: "kitchen-card-head" },
        el("div", {},
          el("p", { className: "eyebrow", text: `Pedido #${pedido.idPedido}` }),
          el("h2", { text: pedido.cliente.nombre }),
          el("p", { className: "muted", text: pedido.fecha })
        ),
        badge(pedido.estado, format.badge(pedido.estado))
      ),
      list,
      el("div", { className: "kitchen-actions" },
        el("button", { className: "btn btn-secondary", text: "Preparando", type: "button", dataset: { id: pedido.idPedido, status: "Preparando" } }),
        el("button", { className: "btn btn-primary", text: "Atendido", type: "button", dataset: { id: pedido.idPedido, status: "Atendido" } })
      )
    );
  }));

  kitchenOrders.querySelectorAll("button").forEach((button) => {
    button.addEventListener("click", () => {
      SalesService.updateOrderStatus(Number(button.dataset.id), button.dataset.status);
      renderKitchen();
    });
  });
}

renderKitchen();
