const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

function authHeaders(extra = {}) {
  const headers = { Accept: "application/json", ...extra };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
  return headers;
}

async function recuperar(id) {
  try {
    const res = await fetch(`/cajero/pedidos/guardados/${id}/recuperar`, {
      method: "POST",
      headers: authHeaders({ "Content-Type": "application/json" }),
    });
    if (!res.ok) throw new Error("No se pudo recuperar el pedido");
    const pedido = await res.json();
    sessionStorage.setItem("mt-pedido-recuperado", JSON.stringify(pedido));
    window.location.href = "/cajero/pos";
  } catch (err) {
    MammaTomatoAlert.error("Error", err.message);
  }
}

function eliminar(id, card) {
  mostrarModalConfirmacion({
    titulo: "Eliminar pedido guardado",
    mensaje: "Estas seguro? Esta accion no se puede deshacer.",
    labelConfirmar: "Eliminar",
    labelCancelar: "Cancelar",
    peligro: true,
    onConfirmar: async () => {
      try {
        const res = await fetch(`/cajero/pedidos/guardados/${id}`, {
          method: "DELETE",
          headers: authHeaders(),
        });
        if (!res.ok) throw new Error("No se pudo eliminar el pedido");

        const lista = document.querySelector(".guardados-lista");
        const restantes = lista ? lista.querySelectorAll(".pedido-guardado-card").length : 0;

        // Si era el ultimo, vamos directo a la pagina vacia (sin toast fugaz)
        if (restantes <= 1) {
          window.location.reload();
          return;
        }

        card.remove();
        MammaTomatoAlert.success("Pedido eliminado", "El pedido fue removido de la lista");
      } catch (err) {
        MammaTomatoAlert.error("Error", err.message);
      }
    },
  });
}

document.querySelectorAll(".btn-recuperar-pedido").forEach((boton) => {
  boton.addEventListener("click", () => recuperar(boton.dataset.id));
});

document.querySelectorAll(".btn-eliminar-guardado").forEach((boton) => {
  boton.addEventListener("click", () => eliminar(boton.dataset.id, boton.closest(".pedido-guardado-card")));
});
