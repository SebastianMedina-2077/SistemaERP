// Cambio 5: bloqueo de sesion del panel admin.
// - Toggle manual en el sidebar: al apagarlo, se bloquea la pantalla.
// - Inactividad: tras 10 minutos sin actividad, se bloquea automaticamente.
// - Desbloqueo: requiere la contrasena del usuario autenticado (verificada en el backend).
(() => {
  "use strict";

  const INACTIVITY_TIMEOUT = 10 * 60 * 1000; // 10 minutos

  const overlay = document.getElementById("lockOverlay");
  const toggle = document.getElementById("sessionToggle");
  const label = document.getElementById("sessionLabel");
  const passwordInput = document.getElementById("lockPassword");
  const errorEl = document.getElementById("lockError");
  const unlockBtn = document.getElementById("lockUnlockBtn");

  // Sin overlay no hay nada que hacer (paginas que no usan el layout admin).
  if (!overlay || !toggle || !passwordInput || !unlockBtn) return;

  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  let inactivityTimer = null;
  let locked = false;

  function setSessionLabel(active) {
    if (!label) return;
    label.textContent = active ? "· sesion activa" : "· sesion bloqueada";
    label.classList.toggle("session-on", active);
    label.classList.toggle("session-off", !active);
  }

  function lock() {
    if (locked) return;
    locked = true;
    clearTimeout(inactivityTimer);
    toggle.checked = false;
    setSessionLabel(false);
    overlay.classList.remove("hidden");
    overlay.setAttribute("aria-hidden", "false");
    passwordInput.value = "";
    errorEl.classList.add("hidden");
    setTimeout(() => passwordInput.focus(), 100);
  }

  function unlock() {
    locked = false;
    overlay.classList.add("hidden");
    overlay.setAttribute("aria-hidden", "true");
    passwordInput.value = "";
    errorEl.classList.add("hidden");
    toggle.checked = true;
    setSessionLabel(true);
    resetInactivityTimer();
  }

  function showError() {
    errorEl.classList.remove("hidden");
    passwordInput.value = "";
    passwordInput.classList.add("shake");
    setTimeout(() => passwordInput.classList.remove("shake"), 450);
    passwordInput.focus();
  }

  async function verificarPassword() {
    const password = passwordInput.value;
    if (!password) { showError(); return; }
    unlockBtn.disabled = true;
    try {
      const headers = { "Content-Type": "application/json", Accept: "application/json" };
      if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
      const res = await fetch("/api/account/verify-password", {
        method: "POST",
        headers,
        body: JSON.stringify({ password }),
      });
      const data = await res.json();
      if (data.valido) unlock();
      else showError();
    } catch (e) {
      errorEl.textContent = "Error de conexion. Intenta de nuevo.";
      errorEl.classList.remove("hidden");
    } finally {
      unlockBtn.disabled = false;
    }
  }

  function resetInactivityTimer() {
    clearTimeout(inactivityTimer);
    if (!locked) {
      inactivityTimer = setTimeout(lock, INACTIVITY_TIMEOUT);
    }
  }

  // Toggle manual: apagarlo bloquea; encenderlo no se permite sin contrasena.
  toggle.addEventListener("change", () => {
    if (!toggle.checked) {
      lock();
    } else if (locked) {
      // No puede reactivarse directamente: debe desbloquear con contrasena.
      toggle.checked = false;
    }
  });

  unlockBtn.addEventListener("click", verificarPassword);
  passwordInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") verificarPassword();
  });

  // Cualquier actividad reinicia el contador de inactividad.
  ["mousedown", "mousemove", "keydown", "scroll", "touchstart", "click"].forEach((evt) => {
    document.addEventListener(evt, () => { if (!locked) resetInactivityTimer(); }, { passive: true });
  });

  setSessionLabel(true);
  resetInactivityTimer();
})();