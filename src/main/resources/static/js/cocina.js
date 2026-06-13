const REFRESH_MS = 8000;

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

const kitchenOrders = document.querySelector("#kitchenOrders");

function badgeClass(estado) {
  const e = String(estado).toLowerCase();
  if (e.includes("atendido")) return "badge badge-success";
  if (e.includes("anulado")) return "badge badge-danger";
  if (e.includes("pendiente")) return "badge badge-warning";
  return "badge badge-info"; // preparando
}

function formatFecha(iso) {
  if (!iso) return "";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : d.toLocaleString("es-PE");
}

async function loadKitchen() {
  try {
    const res = await fetch("/api/pedidos/cocina", { headers: { Accept: "application/json" } });
    if (!res.ok) throw new Error(res.statusText);
    const orders = await res.json();
    renderKitchen(orders);
  } catch (err) {
    kitchenOrders.textContent = `No se pudo cargar la cola: ${err.message}`;
  }
}

function renderKitchen(orders) {
  if (!orders.length) {
    kitchenOrders.className = "kitchen-grid empty-state";
    kitchenOrders.textContent = "No hay pedidos en cola.";
    return;
  }
  kitchenOrders.className = "kitchen-grid";
  kitchenOrders.replaceChildren(...orders.map(renderCard));
}

function renderCard(pedido) {
  const card = document.createElement("article");
  card.className = "kitchen-card";

  const head = document.createElement("div");
  head.className = "kitchen-card-head";
  const info = document.createElement("div");
  info.innerHTML = `<p class="eyebrow"></p><h2></h2><p class="muted"></p>`;
  info.querySelector(".eyebrow").textContent = `Pedido #${pedido.idPedido}`;
  info.querySelector("h2").textContent = pedido.cliente ?? "";
  info.querySelector(".muted").textContent = formatFecha(pedido.fecha);
  const badge = document.createElement("span");
  badge.className = badgeClass(pedido.estado);
  badge.textContent = pedido.estado;
  head.append(info, badge);

  const list = document.createElement("ul");
  list.className = "kitchen-items";
  list.append(...(pedido.items || []).map((item) => {
    const li = document.createElement("li");
    const name = document.createElement("strong");
    name.textContent = item.producto;
    li.append(name, ` x ${item.cantidad}`);
    if (item.observacion) {
      const obs = document.createElement("p");
      obs.className = "muted";
      obs.textContent = item.observacion;
      li.append(obs);
    }
    return li;
  }));

  const actions = document.createElement("div");
  actions.className = "kitchen-actions";
  actions.append(
    actionButton("Preparando", "btn btn-secondary", pedido.idPedido, "PREPARANDO"),
    actionButton("Atendido", "btn btn-primary", pedido.idPedido, "ATENDIDO")
  );

  card.append(head, list, actions);
  return card;
}

function actionButton(text, className, idPedido, estado) {
  const b = document.createElement("button");
  b.type = "button";
  b.className = className;
  b.textContent = text;
  b.addEventListener("click", () => updateStatus(idPedido, estado, b));
  return b;
}

async function updateStatus(idPedido, estado, btn) {
  const headers = { "Content-Type": "application/json", Accept: "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
  btn.disabled = true;
  try {
    const res = await fetch(`/api/pedidos/${idPedido}/estado`, {
      method: "PATCH",
      headers,
      body: JSON.stringify({ estado }),
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.mensaje || res.statusText);
    }
    await loadKitchen();
  } catch (err) {
    alert(`No se pudo actualizar el estado: ${err.message}`);
    btn.disabled = false;
  }
}

loadKitchen();
setInterval(loadKitchen, REFRESH_MS);
