(function () {
    "use strict";

    var tabla = document.querySelector("[data-filtros-pedidos]");
    if (!tabla) {
        return;
    }

    var baseUrl = tabla.getAttribute("data-base-url");
    var controles = tabla.querySelectorAll("[data-filtro]");
    var debounce;

    function navegar() {
        var params = new URLSearchParams();
        controles.forEach(function (control) {
            var valor = control.value ? control.value.trim() : "";
            if (valor) {
                params.set(control.getAttribute("data-filtro"), valor);
            }
        });
        var query = params.toString();
        window.location = query ? baseUrl + "?" + query : baseUrl;
    }

    function esTexto(control) {
        return control.tagName === "INPUT" && (control.type === "text" || control.type === "search");
    }

    controles.forEach(function (control) {
        if (esTexto(control)) {
            control.addEventListener("input", function () {
                clearTimeout(debounce);
                debounce = setTimeout(navegar, 400);
            });
        } else {
            control.addEventListener("change", navegar);
        }
    });

    var limpiar = document.querySelector("[data-limpiar-filtros]");
    if (limpiar) {
        limpiar.addEventListener("click", function (evento) {
            evento.preventDefault();
            window.location = baseUrl;
        });
    }

    // Dropdown compacto por columna: el icono abre el filtro de su encabezado.
    function cerrarPops(excepto) {
        tabla.querySelectorAll(".filtro-pop.open").forEach(function (pop) {
            if (pop !== excepto) {
                pop.classList.remove("open");
            }
        });
    }

    tabla.querySelectorAll(".th-filtrable").forEach(function (th) {
        var toggle = th.querySelector(".filtro-toggle");
        var pop = th.querySelector(".filtro-pop");
        var control = pop ? pop.querySelector("[data-filtro]") : null;
        if (!toggle || !pop || !control) {
            return;
        }
        if (control.value && control.value.trim()) {
            toggle.classList.add("activo");
        }
        if (th.offsetLeft + 210 > tabla.offsetWidth) {
            pop.classList.add("pop-right");
        }
        toggle.addEventListener("click", function (evento) {
            evento.stopPropagation();
            var abierto = pop.classList.contains("open");
            cerrarPops(pop);
            pop.classList.toggle("open", !abierto);
            if (!abierto) {
                control.focus();
            }
        });
        pop.addEventListener("click", function (evento) {
            evento.stopPropagation();
        });
    });

    document.addEventListener("click", function () {
        cerrarPops(null);
    });
}());
