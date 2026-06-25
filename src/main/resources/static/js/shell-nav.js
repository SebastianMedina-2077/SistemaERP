/**
 * SidebarManager: controla el sidebar integrado (cajero y admin).
 * Al colapsarlo se desliza fuera y el contenido se expande (animacion CSS).
 * El estado (abierto/colapsado) se recuerda entre modulos via localStorage.
 *
 * Uso: new SidebarManager("navToggle");  // id del boton de menu (☰)
 */
class SidebarManager {
  static STORAGE_KEY = "mt-sidebar-collapsed";
  static COLLAPSED_CLASS = "nav-collapsed";
  static NO_ANIM_CLASS = "nav-no-anim";

  constructor(toggleId) {
    this.body = document.body;
    this.toggle = document.getElementById(toggleId);
    if (!this.toggle) return; // modulos sin sidebar (ej. cocina)

    this.restaurarEstado();
    this.toggle.addEventListener("click", () => this.alternar());
  }

  /** Aplica el estado guardado sin animar (evita el parpadeo en la carga). */
  restaurarEstado() {
    if (localStorage.getItem(SidebarManager.STORAGE_KEY) !== "true") return;
    this.body.classList.add(SidebarManager.NO_ANIM_CLASS, SidebarManager.COLLAPSED_CLASS);
    requestAnimationFrame(() => this.body.classList.remove(SidebarManager.NO_ANIM_CLASS));
  }

  /** Abre o colapsa el sidebar y persiste el nuevo estado. */
  alternar() {
    const colapsado = this.body.classList.toggle(SidebarManager.COLLAPSED_CLASS);
    localStorage.setItem(SidebarManager.STORAGE_KEY, colapsado);
  }
}

new SidebarManager("navToggle");

// Tras navegar, deja el modulo activo visible en el sidebar (sin recolocar la pagina).
(() => {
  const activo = document.querySelector(".sidebar-nav .nav-item.active, .pos-nav .pos-nav-item.active");
  const nav = activo?.closest(".sidebar-nav, .pos-nav");
  if (!activo || !nav) return;
  const offset = activo.getBoundingClientRect().top - nav.getBoundingClientRect().top;
  nav.scrollTop += offset - (nav.clientHeight - activo.offsetHeight) / 2;
})();

// El cajero no puede salir con la caja abierta: debe cerrar el turno primero.
// El servidor tambien lo bloquea; aqui se avisa al instante sin recargar.
document.querySelectorAll('form[action$="/cajero/salir"]').forEach((form) => {
  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    try {
      const estado = await fetch("/cajero/caja/estado").then((r) => r.json());
      if (estado.abierta) {
        MammaTomatoAlert.warning("Turno activo", "Debes cerrar la caja antes de salir del sistema");
        return;
      }
    } catch (_) {
      // Si falla la verificacion, que decida el guard del servidor.
    }
    form.submit();
  });
});
