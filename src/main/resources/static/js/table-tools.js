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
    const table = card.querySelector("table");

    let page = 0;
    let term = "";
    let lastPage = 0;

    const matchesColumns = setupColumnFilters(table, () => { page = 0; render(); });

    const pager = document.createElement("nav");
    pager.className = "pager-nav d-flex justify-content-between align-items-center flex-wrap gap-2 mt-3";
    pager.setAttribute("aria-label", "Paginacion");
    pager.innerHTML =
      '<span class="muted js-pager-info"></span>' +
      '<div class="d-flex gap-1">' +
      '<button type="button" class="btn btn-ghost btn-sm" data-first aria-label="Primera"><i class="bi bi-chevron-double-left"></i></button>' +
      '<button type="button" class="btn btn-ghost btn-sm" data-prev aria-label="Anterior"><i class="bi bi-chevron-left"></i></button>' +
      '<button type="button" class="btn btn-ghost btn-sm" data-next aria-label="Siguiente"><i class="bi bi-chevron-right"></i></button>' +
      '<button type="button" class="btn btn-ghost btn-sm" data-last aria-label="Ultima"><i class="bi bi-chevron-double-right"></i></button>' +
      '</div>';
    card.insertAdjacentElement("afterend", pager);

    const info = pager.querySelector(".js-pager-info");
    const first = pager.querySelector("[data-first]");
    const prev = pager.querySelector("[data-prev]");
    const next = pager.querySelector("[data-next]");
    const last = pager.querySelector("[data-last]");

    const search = card.id ? document.querySelector(`[data-search-for="${card.id}"]`) : null;
    if (search) {
      search.addEventListener("input", () => {
        term = search.value.trim().toLowerCase();
        page = 0;
        render();
      });
    }

    first.addEventListener("click", () => { page = 0; render(); });
    prev.addEventListener("click", () => { page -= 1; render(); });
    next.addEventListener("click", () => { page += 1; render(); });
    last.addEventListener("click", () => { page = lastPage; render(); });

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
      const visible = all.filter((r) => (!term || rowText(r).includes(term)) && matchesColumns(r));
      const totalPages = Math.max(1, Math.ceil(visible.length / pageSize));
      if (page > totalPages - 1) page = totalPages - 1;
      if (page < 0) page = 0;
      lastPage = totalPages - 1;

      all.forEach((r) => { r.style.display = "none"; });
      visible.slice(page * pageSize, page * pageSize + pageSize).forEach((r) => { r.style.display = ""; });

      info.textContent = `Pagina ${page + 1} de ${totalPages} (${visible.length} registros)`;
      first.disabled = prev.disabled = page <= 0;
      next.disabled = last.disabled = page >= totalPages - 1;
      pager.style.display = totalPages > 1 ? "flex" : "none";
    }

    render();
  }

  // Filtros dropdown por columna (client-side). Se activan marcando un
  // <th data-filter="input|select|range"> en el thead. Devuelve un predicado
  // que indica si una fila pasa todos los filtros de columna activos.
  function setupColumnFilters(table, onChange) {
    const headRow = table ? table.querySelector("thead tr") : null;
    if (!headRow) return () => true;

    const filtros = [];

    [...headRow.children].forEach((th) => {
      const tipo = th.dataset.filter;
      if (!tipo) return;

      const index = th.cellIndex;
      const label = (th.dataset.filterLabel || th.textContent || "").trim();
      const filtro = { index, tipo, valor: null };

      th.classList.add("th-filtrable");
      const toggle = crear("button", "filtro-toggle");
      toggle.type = "button";
      toggle.setAttribute("aria-label", "Filtrar " + label);
      toggle.innerHTML = '<i class="bi bi-funnel"></i>';
      const pop = crear("div", "filtro-pop");

      if (tipo === "select") {
        const sel = crear("select", "form-select form-select-sm");
        sel.innerHTML = '<option value="">Todos</option>';
        valoresUnicos(table, index).forEach((v) => {
          const o = document.createElement("option");
          o.value = v;
          o.textContent = v;
          sel.appendChild(o);
        });
        sel.addEventListener("change", () => {
          filtro.valor = sel.value || null;
          toggle.classList.toggle("activo", !!filtro.valor);
          onChange();
        });
        pop.appendChild(sel);
      } else if (tipo === "range") {
        const cont = crear("div", "filtro-rango");
        cont.innerHTML =
          '<input type="number" class="form-control form-control-sm" placeholder="Min" step="any">' +
          '<input type="number" class="form-control form-control-sm" placeholder="Max" step="any">';
        const [min, max] = cont.querySelectorAll("input");
        const upd = () => {
          const lo = min.value !== "" ? parseFloat(min.value) : null;
          const hi = max.value !== "" ? parseFloat(max.value) : null;
          filtro.valor = lo == null && hi == null ? null : { min: lo, max: hi };
          toggle.classList.toggle("activo", !!filtro.valor);
          onChange();
        };
        min.addEventListener("input", upd);
        max.addEventListener("input", upd);
        pop.appendChild(cont);
      } else {
        const inp = crear("input", "form-control form-control-sm");
        inp.type = "search";
        inp.placeholder = "Buscar " + label.toLowerCase();
        inp.addEventListener("input", () => {
          filtro.valor = inp.value.trim().toLowerCase() || null;
          toggle.classList.toggle("activo", !!filtro.valor);
          onChange();
        });
        pop.appendChild(inp);
      }

      toggle.addEventListener("click", (e) => {
        e.stopPropagation();
        const abierto = pop.classList.contains("open");
        cerrarPops(table);
        pop.classList.toggle("open", !abierto);
      });
      pop.addEventListener("click", (e) => e.stopPropagation());

      th.appendChild(toggle);
      th.appendChild(pop);
      if (th.offsetLeft + 220 > table.offsetWidth) pop.classList.add("pop-right");
      filtros.push(filtro);
    });

    if (filtros.length) {
      document.addEventListener("click", () => cerrarPops(table));
    }

    return (row) => filtros.every((f) => coincide(f, row));
  }

  function coincide(f, row) {
    if (f.valor == null) return true;
    const texto = celdaTexto(row, f.index);
    if (f.tipo === "input") return texto.toLowerCase().includes(f.valor);
    if (f.tipo === "select") return texto === f.valor;
    const n = parseFloat(texto.replace(/[^\d.-]/g, ""));
    if (Number.isNaN(n)) return false;
    if (f.valor.min != null && n < f.valor.min) return false;
    if (f.valor.max != null && n > f.valor.max) return false;
    return true;
  }

  function valoresUnicos(table, index) {
    const filas = [...table.querySelectorAll("tbody tr")].filter((r) => !r.hasAttribute("data-empty"));
    return [...new Set(filas.map((r) => celdaTexto(r, index)).filter(Boolean))].sort();
  }

  function celdaTexto(row, index) {
    const celda = row.children[index];
    return celda ? celda.textContent.trim() : "";
  }

  function cerrarPops(table) {
    table.querySelectorAll(".filtro-pop.open").forEach((p) => p.classList.remove("open"));
  }

  function crear(tag, className) {
    const el = document.createElement(tag);
    el.className = className;
    return el;
  }
})();
