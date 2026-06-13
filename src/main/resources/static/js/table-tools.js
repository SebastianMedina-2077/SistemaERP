// Buscador por nombre + paginacion cliente para tablas largas del admin.
// Convencion: marcar la .table-card con [data-table-tools] (opcional data-page-size),
// y un input con [data-search-for="<id de la table-card>"] junto al titulo.
(() => {
  "use strict";

  document.querySelectorAll("[data-table-tools]").forEach(setupTable);

  function setupTable(card) {
    const pageSize = parseInt(card.dataset.pageSize || "30", 10);
    const tbody = card.querySelector("tbody");
    if (!tbody) return;

    let page = 0;
    let term = "";

    const pager = document.createElement("div");
    pager.className = "table-pager";
    pager.innerHTML =
      '<button type="button" class="pager-btn" data-prev aria-label="Anterior"><i class="bi bi-chevron-left"></i></button>' +
      '<span class="pager-info"></span>' +
      '<button type="button" class="pager-btn" data-next aria-label="Siguiente"><i class="bi bi-chevron-right"></i></button>';
    card.insertAdjacentElement("afterend", pager);

    const info = pager.querySelector(".pager-info");
    const prev = pager.querySelector("[data-prev]");
    const next = pager.querySelector("[data-next]");

    const search = card.id ? document.querySelector(`[data-search-for="${card.id}"]`) : null;
    if (search) {
      search.addEventListener("input", () => {
        term = search.value.trim().toLowerCase();
        page = 0;
        render();
      });
    }

    prev.addEventListener("click", () => { page -= 1; render(); });
    next.addEventListener("click", () => { page += 1; render(); });

    function dataRows() {
      return [...tbody.querySelectorAll("tr")].filter((r) => !r.hasAttribute("data-empty"));
    }

    // Texto buscable: excluye la celda de acciones (la del menu .dropdown)
    function rowText(r) {
      return [...r.children]
        .filter((td) => !td.querySelector(".dropdown"))
        .map((td) => td.textContent)
        .join(" ")
        .toLowerCase();
    }

    function render() {
      const all = dataRows();
      const visible = all.filter((r) => !term || rowText(r).includes(term));
      const totalPages = Math.max(1, Math.ceil(visible.length / pageSize));
      if (page > totalPages - 1) page = totalPages - 1;
      if (page < 0) page = 0;

      all.forEach((r) => { r.style.display = "none"; });
      visible.slice(page * pageSize, page * pageSize + pageSize).forEach((r) => { r.style.display = ""; });

      info.textContent = visible.length ? `${page + 1} de ${totalPages}` : "0 de 0";
      prev.disabled = page <= 0;
      next.disabled = page >= totalPages - 1;
      pager.style.display = totalPages > 1 ? "flex" : "none";
    }

    render();
  }
})();
