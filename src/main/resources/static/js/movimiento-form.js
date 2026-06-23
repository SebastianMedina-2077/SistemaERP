(function () {
  const root = document.querySelector("[data-movimiento-lines]");
  const datalist = document.getElementById("insumosList");
  if (!root || !datalist) return;

  const options = Array.from(datalist.options);
  const items = options.map((option) => ({
    code: option.value,
    id: option.dataset.id,
    name: option.dataset.nombre,
    stock: option.dataset.stock,
    measure: option.dataset.medida,
    search: `${option.value} ${option.dataset.nombre}`.toUpperCase()
  }));
  const modalEl = document.getElementById("insumoSearchModal");
  const searchInput = document.getElementById("insumoSearchInput");
  const results = document.querySelector("[data-insumo-results]");
  const modal = modalEl && window.bootstrap ? new bootstrap.Modal(modalEl) : null;
  let activeLine = null;

  function findByCode(code) {
    return options.find((option) => option.value.toUpperCase() === code.trim().toUpperCase());
  }

  function refreshLine(line) {
    const code = line.querySelector(".mov-code");
    const id = line.querySelector(".mov-id");
    const name = line.querySelector(".mov-name");
    const stock = line.querySelector(".mov-stock");
    const option = findByCode(code.value);

    if (!option) {
      id.value = "";
      name.value = "";
      stock.value = "";
      return;
    }

    id.value = option.dataset.id;
    name.value = option.dataset.nombre;
    stock.value = `${option.dataset.stock} ${option.dataset.medida}`;
  }

  function isLineComplete(line) {
    const id = line.querySelector(".mov-id");
    const cantidad = line.querySelector("input[type='number']");
    return Boolean(id.value) && cantidad.value !== "" && cantidad.checkValidity();
  }

  function isLastLine(line) {
    const lines = Array.from(root.querySelectorAll("[data-line]"));
    return lines[lines.length - 1] === line;
  }

  function focusQuantity(line) {
    const cantidad = line.querySelector("input[type='number']");
    if (cantidad) cantidad.focus();
  }

  function addLineAfterIfComplete(line) {
    if (!isLastLine(line) || !isLineComplete(line)) return;
    const nextLine = createLine();
    root.insertBefore(nextLine, root.querySelector("[data-line-error]"));
    reindexLines();
    nextLine.querySelector(".mov-code").focus();
  }

  function reindexLines() {
    root.querySelectorAll("[data-line]").forEach((line, index) => {
      const id = line.querySelector(".mov-id");
      const cantidad = line.querySelector("input[type='number']");
      id.name = `lineas[${index}].idInsumo`;
      id.id = `lineas${index}.idInsumo`;
      cantidad.name = `lineas[${index}].cantidad`;
      cantidad.id = `lineas${index}.cantidad`;
    });
  }

  function createLine() {
    const first = root.querySelector("[data-line]");
    const line = first.cloneNode(true);
    line.querySelectorAll("input").forEach((input) => {
      input.value = "";
      input.classList.remove("is-invalid");
    });
    line.querySelectorAll(".invalid-feedback").forEach((feedback) => {
      feedback.textContent = "";
    });
    return line;
  }

  function renderResults(term = "") {
    if (!results) return;

    const normalizedTerm = term.trim().toUpperCase();
    const matches = normalizedTerm
      ? items.filter((item) => item.search.includes(normalizedTerm))
      : items;

    results.textContent = "";

    if (matches.length === 0) {
      const row = document.createElement("tr");
      const cell = document.createElement("td");
      cell.colSpan = 4;
      cell.className = "text-center muted-sm";
      cell.textContent = "No se encontraron insumos.";
      row.appendChild(cell);
      results.appendChild(row);
      return;
    }

    const fragment = document.createDocumentFragment();
    matches.forEach((item) => {
      const row = document.createElement("tr");
      row.dataset.insumoCode = item.code;

      const codeCell = document.createElement("td");
      const codeText = document.createElement("strong");
      codeText.textContent = item.code;
      codeCell.appendChild(codeText);

      const nameCell = document.createElement("td");
      nameCell.textContent = item.name;

      const stockCell = document.createElement("td");
      stockCell.textContent = `${item.stock} ${item.measure}`;

      const actionCell = document.createElement("td");
      actionCell.className = "text-right";
      const selectButton = document.createElement("button");
      selectButton.className = "btn btn-secondary btn-sm";
      selectButton.type = "button";
      selectButton.dataset.selectInsumo = item.code;
      selectButton.textContent = "Seleccionar";
      actionCell.appendChild(selectButton);

      row.append(codeCell, nameCell, stockCell, actionCell);
      fragment.appendChild(row);
    });
    results.appendChild(fragment);
  }

  function openSearch(line) {
    if (!modal) return;
    activeLine = line;
    renderResults(searchInput.value);
    modal.show();
  }

  function selectInsumo(code) {
    if (!activeLine) return;

    const codeInput = activeLine.querySelector(".mov-code");
    codeInput.value = code;
    refreshLine(activeLine);
    modal.hide();
    focusQuantity(activeLine);
    addLineAfterIfComplete(activeLine);
  }

  root.addEventListener("input", (event) => {
    const line = event.target.closest("[data-line]");
    if (event.target.classList.contains("mov-code") && line) {
      refreshLine(line);
      addLineAfterIfComplete(line);
      return;
    }

    if (event.target.matches("input[type='number']") && line) {
      addLineAfterIfComplete(line);
    }
  });

  root.addEventListener("click", (event) => {
    const addButton = event.target.closest("[data-add-line]");
    const removeButton = event.target.closest("[data-remove-line]");
    const searchButton = event.target.closest("[data-search-insumo]");

    if (addButton) {
      root.insertBefore(createLine(), root.querySelector("[data-line-error]"));
      reindexLines();
      return;
    }

    if (removeButton) {
      const lines = root.querySelectorAll("[data-line]");
      if (lines.length > 1) {
        removeButton.closest("[data-line]").remove();
        reindexLines();
      }
    }

    if (searchButton) {
      const line = searchButton.closest("[data-line]");
      if (line) openSearch(line);
    }
  });

  if (searchInput) {
    searchInput.addEventListener("input", () => renderResults(searchInput.value));
  }

  if (modalEl) {
    modalEl.addEventListener("shown.bs.modal", () => {
      searchInput.value = "";
      renderResults();
      searchInput.focus();
    });

    modalEl.addEventListener("hidden.bs.modal", () => {
      activeLine = null;
    });
  }

  if (results) {
    results.addEventListener("click", (event) => {
      const button = event.target.closest("[data-select-insumo]");
      const row = event.target.closest("[data-insumo-code]");
      const code = button ? button.dataset.selectInsumo : row?.dataset.insumoCode;
      if (code) selectInsumo(code);
    });
  }

  root.querySelectorAll("[data-line]").forEach((line) => {
    const id = line.querySelector(".mov-id");
    const selected = options.find((option) => option.dataset.id === id.value);
    if (selected) {
      line.querySelector(".mov-code").value = selected.value;
      refreshLine(line);
    }
  });

  renderResults();
})();
