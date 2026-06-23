(function () {
  const root = document.querySelector("[data-movimiento-lines]");
  const datalist = document.getElementById("insumosList");
  if (!root || !datalist) return;

  const options = Array.from(datalist.options);

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

  root.addEventListener("input", (event) => {
    const line = event.target.closest("[data-line]");
    if (event.target.classList.contains("mov-code") && line) {
      refreshLine(line);
    }
  });

  root.addEventListener("click", (event) => {
    const addButton = event.target.closest("[data-add-line]");
    const removeButton = event.target.closest("[data-remove-line]");

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
  });

  root.querySelectorAll("[data-line]").forEach((line) => {
    const id = line.querySelector(".mov-id");
    const selected = options.find((option) => option.dataset.id === id.value);
    if (selected) {
      line.querySelector(".mov-code").value = selected.value;
      refreshLine(line);
    }
  });
})();
