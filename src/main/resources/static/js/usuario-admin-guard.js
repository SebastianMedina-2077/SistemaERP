// Cambio 6: al crear un usuario con rol Administrador, pide la contrasena
// del administrador actual antes de enviar el formulario.
(() => {
  "use strict";

  const form = document.getElementById("usuarioForm");
  const rolSelect = document.getElementById("rolSelect");
  const modal = document.getElementById("confirmAdminModal");
  const pwd = document.getElementById("adminConfirmPassword");
  const err = document.getElementById("adminConfirmError");
  const confirmBtn = document.getElementById("adminConfirmBtn");
  const cancelBtn = document.getElementById("adminConfirmCancel");

  if (!form || !rolSelect || !modal) return;

  const esNuevo = form.dataset.nuevo === "true";
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  let confirmado = false;

  function rolEsAdministrador() {
    const opt = rolSelect.options[rolSelect.selectedIndex];
    return opt && opt.text.trim().toLowerCase() === "administrador";
  }

  function mostrarError(msg) {
    err.textContent = msg || "Contrasena incorrecta.";
    err.classList.remove("hidden");
    pwd.value = "";
    pwd.classList.add("shake");
    setTimeout(() => pwd.classList.remove("shake"), 450);
    pwd.focus();
  }

  function cerrarModal() {
    modal.classList.add("hidden");
    modal.setAttribute("aria-hidden", "true");
    pwd.value = "";
    err.classList.add("hidden");
  }

  form.addEventListener("submit", (e) => {
    if (confirmado) return;            // ya confirmado: dejar enviar
    if (esNuevo && rolEsAdministrador()) {
      e.preventDefault();
      pwd.value = "";
      err.classList.add("hidden");
      modal.classList.remove("hidden");
      modal.setAttribute("aria-hidden", "false");
      setTimeout(() => pwd.focus(), 100);
    }
  });

  async function confirmar() {
    const password = pwd.value;
    if (!password) { mostrarError("Ingresa tu contrasena."); return; }
    confirmBtn.disabled = true;
    try {
      const headers = { "Content-Type": "application/json", Accept: "application/json" };
      if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
      const res = await fetch("/api/account/verify-password", {
        method: "POST",
        headers,
        body: JSON.stringify({ password }),
      });
      const data = await res.json();
      if (data.valido) {
        confirmado = true;
        cerrarModal();
        form.submit();
      } else {
        mostrarError();
      }
    } catch (e) {
      mostrarError("Error de conexion. Intenta de nuevo.");
    } finally {
      confirmBtn.disabled = false;
    }
  }

  confirmBtn.addEventListener("click", confirmar);
  cancelBtn.addEventListener("click", cerrarModal);
  pwd.addEventListener("keydown", (e) => { if (e.key === "Enter") confirmar(); });
})();
