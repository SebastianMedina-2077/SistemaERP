// Cerrar Dia: carga el resumen del dia y confirma el cierre (endpoint aun stub).
(function () {
  "use strict";

  const boton = document.getElementById("btnCerrarDia");
  const modalEl = document.getElementById("modalCierreDia");
  if (!boton || !modalEl || !window.bootstrap) return;

  const modal = new bootstrap.Modal(modalEl);
  const resumen = document.getElementById("resumenCierreDia");
  const mensaje = document.getElementById("cierreMensaje");
  const confirmar = document.getElementById("btnConfirmarCierre");

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";

  function pintarResumen(data) {
    const ventas = Number(data.totalVentas || 0).toFixed(2);
    resumen.innerHTML =
      '<div class="resumen-item"><span>Fecha</span><strong>' + data.fecha + "</strong></div>" +
      '<div class="resumen-item"><span>Pedidos del dia</span><strong>' + data.totalPedidos + "</strong></div>" +
      '<div class="resumen-item"><span>Total vendido</span><strong>S/ ' + ventas + "</strong></div>";
  }

  boton.addEventListener("click", function () {
    mensaje.classList.add("hidden");
    confirmar.disabled = false;
    resumen.innerHTML = '<div class="resumen-item"><span>Cargando resumen...</span></div>';
    modal.show();
    fetch("/admin/caja/resumen-dia", { headers: { Accept: "application/json" } })
      .then(function (res) { return res.json(); })
      .then(pintarResumen)
      .catch(function () {
        resumen.innerHTML = '<div class="resumen-item"><span>No se pudo cargar el resumen.</span></div>';
      });
  });

  confirmar.addEventListener("click", function () {
    confirmar.disabled = true;
    fetch("/admin/caja/cerrar-dia", {
      method: "POST",
      headers: (function () {
        const h = { Accept: "application/json" };
        if (csrfToken) h[csrfHeader] = csrfToken;
        return h;
      })()
    })
      .then(function (res) { return res.json(); })
      .then(function (data) {
        mensaje.textContent = data.mensaje || "Operacion completada.";
        mensaje.classList.remove("hidden");
        mensaje.classList.toggle("cierre-mensaje-ok", data.ok === true);
        mensaje.classList.toggle("cierre-mensaje-info", data.ok !== true);
      })
      .catch(function () {
        mensaje.textContent = "Error al cerrar el dia. Intenta de nuevo.";
        mensaje.classList.remove("hidden");
        mensaje.classList.add("cierre-mensaje-info");
        confirmar.disabled = false;
      });
  });
})();
