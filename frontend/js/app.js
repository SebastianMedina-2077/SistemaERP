import { AuthService } from "./services.js";

export function setupUserBox(fallbackRole) {
  const session = AuthService.getSession(fallbackRole);
  const activeUser = document.querySelector("#activeUser");
  const activeRole = document.querySelector("#activeRole");
  const logoutBtn = document.querySelector("#logoutBtn");

  if (activeUser) activeUser.textContent = session.nombre;
  if (activeRole) activeRole.textContent = session.rol;
  if (logoutBtn) logoutBtn.addEventListener("click", () => AuthService.logout());
}

export function clear(node) {
  if (node) node.replaceChildren();
}

export function el(tag, options = {}, ...children) {
  const node = document.createElement(tag);
  if (options.className) node.className = options.className;
  if (options.text !== undefined) node.textContent = options.text;
  if (options.type) node.type = options.type;
  if (options.value !== undefined) node.value = options.value;
  if (options.placeholder) node.placeholder = options.placeholder;
  if (options.dataset) {
    Object.entries(options.dataset).forEach(([key, value]) => {
      node.dataset[key] = value;
    });
  }
  children.flat().filter(Boolean).forEach((child) => {
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  });
  return node;
}

export function cell(content) {
  const td = document.createElement("td");
  if (content instanceof Node) td.append(content);
  else td.textContent = content ?? "";
  return td;
}

export function row(...cells) {
  const tr = document.createElement("tr");
  tr.append(...cells);
  return tr;
}

export function badge(text, className) {
  return el("span", { className, text });
}

export function textButton(text, className = "btn btn-ghost", dataset = {}) {
  return el("button", { className, text, type: "button", dataset });
}

export function actionButtons({ includeAnular = false, includeDetalle = false, includeBoleta = false } = {}) {
  const wrap = el("div", { className: "action-row" });
  if (includeDetalle) wrap.append(textButton("Ver detalle", "btn btn-secondary js-detail"));
  if (includeBoleta) wrap.append(textButton("Ver boleta", "btn btn-secondary js-ticket"));
  wrap.append(textButton("Editar"), textButton("Desactivar", "btn btn-secondary"));
  if (includeAnular) wrap.append(textButton("Anular", "btn btn-danger"));
  return wrap;
}

export function appendRows(target, rows) {
  clear(target);
  target.append(...rows);
}

export function fillSelect(select, items, getValue, getText, firstLabel = "") {
  clear(select);
  if (firstLabel) select.append(el("option", { text: firstLabel }));
  select.append(...items.map((item) => {
    const option = el("option", { text: getText(item) });
    option.value = getValue(item);
    return option;
  }));
}
