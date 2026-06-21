// Tabs client-side de Reportes: alterna los paneles sin recargar.
(() => {
  "use strict";

  const tabs = [...document.querySelectorAll(".rep-tab")];
  const paneles = [...document.querySelectorAll(".rep-panel")];
  if (!tabs.length) return;

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => {
      const objetivo = tab.dataset.tab;
      tabs.forEach((t) => t.classList.toggle("activo", t === tab));
      paneles.forEach((p) => p.classList.toggle("hidden", p.dataset.panel !== objetivo));
    });
  });
})();
