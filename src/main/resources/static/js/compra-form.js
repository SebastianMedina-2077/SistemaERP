(function () {
  const root = document.querySelector("[data-compra-lines]");
  if (!root) return;

  const totalEl = document.querySelector("[data-compra-total]");

  function toNumber(value) {
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  function money(value) {
    return `S/ ${value.toFixed(2)}`;
  }

  function calculateLine(line) {
    const cantidad = toNumber(line.querySelector(".compra-cantidad")?.value);
    const precio = toNumber(line.querySelector(".compra-precio")?.value);
    const subtotal = cantidad * precio;
    const subtotalInput = line.querySelector(".compra-subtotal");
    if (subtotalInput) subtotalInput.value = money(subtotal);
    return subtotal;
  }

  function calculateTotal() {
    const total = Array.from(root.querySelectorAll("[data-compra-line]"))
      .reduce((sum, line) => sum + calculateLine(line), 0);
    if (totalEl) totalEl.textContent = money(total);
  }

  function reindexLines() {
    root.querySelectorAll("[data-compra-line]").forEach((line, index) => {
      const insumo = line.querySelector(".compra-insumo");
      const cantidad = line.querySelector(".compra-cantidad");
      const precio = line.querySelector(".compra-precio");

      insumo.name = `items[${index}].idInsumo`;
      insumo.id = `items${index}.idInsumo`;
      cantidad.name = `items[${index}].cantidad`;
      cantidad.id = `items${index}.cantidad`;
      precio.name = `items[${index}].precioUnitario`;
      precio.id = `items${index}.precioUnitario`;
    });
  }

  function createLine() {
    const first = root.querySelector("[data-compra-line]");
    const line = first.cloneNode(true);

    line.querySelectorAll("select, input").forEach((field) => {
      if (field.classList.contains("compra-subtotal")) {
        field.value = "S/ 0.00";
      } else {
        field.value = "";
      }
      field.classList.remove("is-invalid");
    });
    line.querySelectorAll(".invalid-feedback").forEach((feedback) => {
      feedback.textContent = "";
    });
    return line;
  }

  root.addEventListener("input", (event) => {
    if (event.target.matches(".compra-cantidad, .compra-precio")) {
      calculateTotal();
    }
  });

  root.addEventListener("change", (event) => {
    if (!event.target.classList.contains("compra-insumo")) return;

    const selected = event.target.selectedOptions[0];
    const line = event.target.closest("[data-compra-line]");
    const precio = line?.querySelector(".compra-precio");
    if (precio && !precio.value && selected?.dataset.precio) {
      precio.value = Number.parseFloat(selected.dataset.precio).toFixed(2);
    }
    calculateTotal();
  });

  root.addEventListener("click", (event) => {
    const addButton = event.target.closest("[data-add-compra-line]");
    const removeButton = event.target.closest("[data-remove-compra-line]");

    if (addButton) {
      const nextLine = createLine();
      root.insertBefore(nextLine, root.querySelector("[data-compra-line-error]"));
      reindexLines();
      nextLine.querySelector(".compra-insumo")?.focus();
      calculateTotal();
      return;
    }

    if (removeButton) {
      const lines = root.querySelectorAll("[data-compra-line]");
      if (lines.length > 1) {
        removeButton.closest("[data-compra-line]").remove();
        reindexLines();
        calculateTotal();
      }
    }
  });

  calculateTotal();
})();
