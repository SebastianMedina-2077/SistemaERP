/* Tienda en línea Mamma Tomato.
   Regla del proyecto: el total definitivo SIEMPRE lo calcula el backend
   (POST /api/tienda/cotizar); los subtotales locales son solo referenciales. */

const API = {
  catalogo: "/api/tienda/catalogo",
  cotizar: "/api/tienda/cotizar",
  registro: "/api/tienda/auth/registro",
  ingreso: "/api/tienda/auth/login",
  cuenta: "/api/tienda/cuenta",
  pedidos: "/api/tienda/pedidos",
  promoValidar: "/api/tienda/promo/validar",
};

const CLAVE_CARRITO = "tienda.carrito";
const CLAVE_SESION = "tienda.sesion";
const CLAVE_MODALIDAD = "tienda.modalidad";

const soles = (v) => `S/ ${Number(v || 0).toFixed(2)}`;
const centavos = (v) => Math.round(Number(v || 0) * 100);
const movimientoReducido = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

function rebotar(fn, espera) {
  let temporizador;
  return (...args) => {
    clearTimeout(temporizador);
    temporizador = setTimeout(() => fn(...args), espera);
  };
}

// ---------- HTTP ----------
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

async function peticion(metodo, url, cuerpo, conSesion = false) {
  const headers = { Accept: "application/json" };
  if (cuerpo !== undefined) headers["Content-Type"] = "application/json";
  if (metodo !== "GET" && csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
  if (conSesion && sesion?.token) headers.Authorization = `Bearer ${sesion.token}`;

  let res;
  try {
    res = await fetch(url, { method: metodo, headers, body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined });
  } catch {
    throw Object.assign(new Error("Sin conexión con el servidor. Revisa tu internet."), { deRed: true });
  }
  if (!res.ok) {
    let datos = null;
    try { datos = await res.json(); } catch { /* respuesta sin cuerpo */ }
    if (res.status === 401 && conSesion) {
      cerrarSesion(true);
      throw Object.assign(new Error("Tu sesión expiró. Vuelve a ingresar."), { status: 401, datos });
    }
    throw Object.assign(new Error(datos?.error || `Error ${res.status}`), { status: res.status, datos });
  }
  if (res.status === 204) return null;
  return res.json();
}

// ---------- Estado ----------
let catalogo = { categorias: [], productos: [], promociones: [] };
let carrito = new Map(); // idProducto -> cantidad
let sesion = null;       // { token, nombre, email }
let modalidad = "DELIVERY";
let cotizacion = null;   // última respuesta válida de /cotizar
let secuenciaCotizacion = 0;
let tarjetasGuardadas = [];
let categoriaActiva = "";
let termino = "";
let cuponCodigo = null; // código aplicado: viaja en /cotizar y en el pedido (el descuento lo decide el servidor)

function cargarEstadoGuardado() {
  try {
    const crudo = JSON.parse(localStorage.getItem(CLAVE_CARRITO) || "[]");
    if (Array.isArray(crudo)) crudo.forEach(([id, cant]) => {
      if (Number(id) > 0 && Number(cant) > 0) carrito.set(Number(id), Math.min(99, Number(cant)));
    });
  } catch { carrito = new Map(); }
  // La sesión vive en localStorage ("recordarme") o en sessionStorage (solo esta pestaña)
  try {
    sesion = JSON.parse(localStorage.getItem(CLAVE_SESION) || sessionStorage.getItem(CLAVE_SESION)) || null;
  } catch { sesion = null; }
  if (sesion && !sesion.token) sesion = null;
  const guardada = localStorage.getItem(CLAVE_MODALIDAD);
  if (guardada === "DELIVERY" || guardada === "RECOJO") modalidad = guardada;
}

function persistirCarrito() {
  localStorage.setItem(CLAVE_CARRITO, JSON.stringify([...carrito.entries()]));
}

// ---------- Avisos (toasts) ----------
const zonaAvisos = document.querySelector("#zonaAvisos");

function mostrarAviso(mensaje, tipo = "info") {
  const aviso = document.createElement("div");
  aviso.className = `aviso${tipo === "error" ? " aviso-error" : tipo === "exito" ? " aviso-exito" : ""}`;
  aviso.setAttribute("role", "status");
  const icono = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  icono.classList.add("icono");
  icono.innerHTML = `<use href="#icono-${tipo === "error" ? "alerta" : tipo === "exito" ? "check" : "alerta"}"/>`;
  aviso.append(icono, document.createTextNode(mensaje));
  zonaAvisos.append(aviso);
  setTimeout(() => aviso.remove(), 4500);
}

// ---------- Capas (modales y drawer): foco, Escape, fondo ----------
const capasAbiertas = [];

function abrirCapa(idCapa, idFondo) {
  const capa = document.getElementById(idCapa);
  const fondo = document.getElementById(idFondo);
  capa.hidden = false;
  fondo.hidden = false;
  capasAbiertas.push({ capa, fondo, focoPrevio: document.activeElement });
  document.body.style.overflow = "hidden";
  const enfocable = capa.querySelector("input:not([hidden]), button:not(.modal-cerrar):not([hidden]), select");
  (enfocable || capa).focus?.();
  fondo.onclick = () => cerrarCapa(idCapa);
}

function cerrarCapa(idCapa) {
  const indice = capasAbiertas.findIndex((c) => c.capa.id === idCapa);
  if (indice < 0) return;
  const { capa, fondo, focoPrevio } = capasAbiertas[indice];
  capasAbiertas.splice(indice, 1);
  capa.hidden = true;
  fondo.hidden = true;
  if (!capasAbiertas.length) document.body.style.overflow = "";
  focoPrevio?.focus?.();
}

document.addEventListener("keydown", (ev) => {
  if (ev.key !== "Escape" || !capasAbiertas.length) return;
  cerrarCapa(capasAbiertas[capasAbiertas.length - 1].capa.id);
});

document.querySelectorAll(".modal-cerrar").forEach((btn) => {
  btn.addEventListener("click", () => cerrarCapa(btn.dataset.cerrar));
});

// ---------- Cabecera ----------
const btnBuscarMovil = document.querySelector("#btnBuscarMovil");
const filaBuscadorMovil = document.querySelector("#filaBuscadorMovil");
const contadorCarrito = document.querySelector("#contadorCarrito");
const totalCabecera = document.querySelector("#totalCabecera");
const btnCuenta = document.querySelector("#btnCuenta");
const etiquetaCuenta = document.querySelector("#etiquetaCuenta");
const menuCuenta = document.querySelector("#menuCuenta");

btnBuscarMovil.addEventListener("click", () => {
  const abierto = filaBuscadorMovil.hidden;
  filaBuscadorMovil.hidden = !abierto;
  btnBuscarMovil.setAttribute("aria-expanded", String(abierto));
  if (abierto) document.querySelector("#buscadorMovil").focus();
});

btnCuenta.addEventListener("click", () => {
  if (!sesion) { location.href = "/tienda/ingresar"; return; }
  menuCuenta.hidden = !menuCuenta.hidden;
});

document.addEventListener("click", (ev) => {
  if (!menuCuenta.hidden && !ev.target.closest(".cuenta-zona")) menuCuenta.hidden = true;
});

document.querySelector("#btnCerrarSesion").addEventListener("click", () => {
  cerrarSesion(false);
  mostrarAviso("Cerraste tu sesión.", "exito");
});

const CLASE_ESTADO = {
  PENDIENTE: "estado-pendiente",
  PREPARANDO: "estado-preparacion",
  ATENDIDO: "estado-atendido",
  ANULADO: "estado-anulado",
};

document.querySelector("#btnMisPedidos").addEventListener("click", () => {
  menuCuenta.hidden = true;
  abrirCapa("modalPedidos", "fondoPedidos");
  cargarMisPedidos();
});

async function cargarMisPedidos() {
  const lista = document.querySelector("#listaPedidos");
  lista.replaceChildren(avisoPedidos("Cargando tus pedidos…"));
  try {
    const pedidos = await peticion("GET", API.pedidos, undefined, true);
    if (!pedidos.length) {
      lista.replaceChildren(avisoPedidos("Aún no tienes pedidos. ¡Arma tu primera pizza!"));
      return;
    }
    lista.replaceChildren(...pedidos.map(tarjetaPedido));
  } catch (err) {
    if (err.status === 401) return; // la sesión expiró: peticion() ya lo gestiona
    const cont = avisoPedidos("No pudimos cargar tus pedidos. ", "pedidos-error");
    const reintentar = document.createElement("button");
    reintentar.type = "button";
    reintentar.className = "boton boton-fantasma";
    reintentar.textContent = "Reintentar";
    reintentar.addEventListener("click", cargarMisPedidos);
    cont.append(reintentar);
    lista.replaceChildren(cont);
  }
}

function avisoPedidos(texto, clase = "pedidos-vacio") {
  const div = document.createElement("div");
  div.className = clase;
  div.append(texto);
  return div;
}

function tarjetaPedido(p) {
  const art = document.createElement("article");
  art.className = "tarjeta-pedido";

  const cabecera = document.createElement("div");
  cabecera.className = "tarjeta-pedido-cabecera";
  const numero = document.createElement("span");
  numero.className = "tarjeta-pedido-numero";
  numero.textContent = `N° ${p.idPedido}`;
  const estado = document.createElement("span");
  estado.className = `insignia-estado ${CLASE_ESTADO[p.estado] || "estado-otro"}`;
  estado.textContent = (p.estado || "").replace(/_/g, " ");
  cabecera.append(numero, estado);

  const fecha = document.createElement("p");
  fecha.className = "tarjeta-pedido-fecha";
  fecha.textContent = formatearFecha(p.fecha);

  const items = document.createElement("p");
  items.className = "tarjeta-pedido-items";
  items.textContent = (p.items || []).map((i) => `${i.cantidad}× ${i.nombre}`).join(", ");

  const pie = document.createElement("div");
  pie.className = "tarjeta-pedido-pie";
  const modalidad = document.createElement("span");
  modalidad.className = "tarjeta-pedido-modalidad";
  modalidad.textContent = p.modalidad === "DELIVERY" ? "Delivery" : "Recojo en tienda";
  const total = document.createElement("span");
  total.className = "tarjeta-pedido-total";
  total.textContent = soles(p.total);
  pie.append(modalidad, total);

  art.append(cabecera, fecha, items, pie);
  return art;
}

function formatearFecha(iso) {
  if (!iso) return "";
  const fecha = new Date(iso);
  if (isNaN(fecha.getTime())) return iso;
  return fecha.toLocaleString("es-PE", {
    day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
  });
}

function pintarSesion() {
  if (sesion) {
    etiquetaCuenta.textContent = sesion.nombre?.split(" ")[0] || "Mi cuenta";
    document.querySelector("#menuCuentaNombre").textContent = sesion.nombre || "";
    document.querySelector("#menuCuentaEmail").textContent = sesion.email || "";
  } else {
    etiquetaCuenta.textContent = "Iniciar sesión";
  }
}

function cerrarSesion(porExpiracion) {
  sesion = null;
  tarjetasGuardadas = [];
  localStorage.removeItem(CLAVE_SESION);
  sessionStorage.removeItem(CLAVE_SESION);
  menuCuenta.hidden = true;
  pintarSesion();
  if (porExpiracion) {
    mostrarAviso("Tu sesión expiró. Vuelve a ingresar.", "error");
    setTimeout(() => { location.href = "/tienda/ingresar"; }, 1200);
  }
}

function actualizarIndicadores() {
  let unidades = 0;
  let totalReferencial = 0;
  carrito.forEach((cant, id) => {
    unidades += cant;
    const producto = catalogo.productos.find((p) => p.id === id);
    if (producto) totalReferencial += producto.precio * cant;
  });
  contadorCarrito.textContent = unidades;
  totalCabecera.textContent = soles(totalReferencial);
  document.querySelector("#btnCarrito").setAttribute(
    "aria-label",
    `Abrir carrito, ${unidades} producto${unidades === 1 ? "" : "s"}`
  );
}

// ---------- Modalidad ----------
function pintarModalidad() {
  document.querySelectorAll(".tab-modalidad").forEach((tab) => {
    const activa = tab.dataset.modalidad === modalidad;
    tab.classList.toggle("activa", activa);
    tab.setAttribute("aria-pressed", String(activa));
  });
  document.querySelector("#bloqueDireccion").hidden = modalidad !== "DELIVERY";
  document.querySelector("#notaRecojo").hidden = modalidad !== "RECOJO";
}

document.querySelectorAll(".tab-modalidad").forEach((tab) => {
  tab.addEventListener("click", () => {
    modalidad = tab.dataset.modalidad;
    localStorage.setItem(CLAVE_MODALIDAD, modalidad);
    pintarModalidad();
  });
});

// ---------- Carrusel ----------
const carrusel = document.querySelector("#carrusel");
const pista = document.querySelector("#carruselPista");
const puntosContenedor = document.querySelector("#carruselPuntos");
let totalDiapositivas = pista.children.length;
let diapositivaActual = 0;
let autoplayId = null;
const INTERVALO_CARRUSEL = 7000; // autoavance fijo; sin botón de pausa (solo se detiene con movimiento reducido)

function irADiapositiva(indice) {
  diapositivaActual = (indice + totalDiapositivas) % totalDiapositivas;
  pista.style.transform = `translateX(-${diapositivaActual * 100}%)`;
  puntosContenedor.querySelectorAll(".carrusel-punto").forEach((punto, i) => {
    punto.classList.toggle("activo", i === diapositivaActual);
    punto.setAttribute("aria-selected", String(i === diapositivaActual));
  });
}

function construirPuntos() {
  puntosContenedor.replaceChildren();
  for (let i = 0; i < totalDiapositivas; i++) {
    const punto = document.createElement("button");
    punto.type = "button";
    punto.className = "carrusel-punto";
    punto.setAttribute("role", "tab");
    punto.setAttribute("aria-label", `Promoción ${i + 1}`);
    punto.addEventListener("click", () => { irADiapositiva(i); reiniciarAutoplay(); });
    puntosContenedor.append(punto);
  }
}
construirPuntos();

/* El primer slide es el banner oficial; los demás toman el texto de las
   promociones reales del catálogo (los sobrantes se retiran). */
function pintarHeroPromos() {
  const plantillas = [...pista.querySelectorAll("[data-plantilla-promo]")];
  const promos = catalogo.promociones || [];
  plantillas.forEach((diapositiva, i) => {
    const promo = promos[i];
    if (!promo) { diapositiva.remove(); return; }
    diapositiva.querySelector(".insignia-promo").textContent = "Promoción activa";
    diapositiva.querySelector("h2").textContent = promo.descripcion;
    diapositiva.querySelector("p").textContent = "El descuento se aplica solo al cotizar tu pedido.";
  });

  totalDiapositivas = pista.children.length;
  pista.querySelectorAll(".diapositiva").forEach((d, i) => {
    d.setAttribute("aria-label", `Promoción ${i + 1} de ${totalDiapositivas}`);
  });
  construirPuntos();

  const unica = totalDiapositivas <= 1;
  document.querySelector("#carruselAnterior").hidden = unica;
  document.querySelector("#carruselSiguiente").hidden = unica;
  document.querySelector("#carruselPie").hidden = unica;
  irADiapositiva(0);
  if (unica) detenerAutoplay();
  else reiniciarAutoplay();
}

function arrancarAutoplay() {
  if (movimientoReducido || autoplayId) return;
  autoplayId = setInterval(() => irADiapositiva(diapositivaActual + 1), INTERVALO_CARRUSEL);
}
function detenerAutoplay() {
  clearInterval(autoplayId);
  autoplayId = null;
}
function reiniciarAutoplay() {
  detenerAutoplay();
  arrancarAutoplay();
}

document.querySelector("#carruselAnterior").addEventListener("click", () => { irADiapositiva(diapositivaActual - 1); reiniciarAutoplay(); });
document.querySelector("#carruselSiguiente").addEventListener("click", () => { irADiapositiva(diapositivaActual + 1); reiniciarAutoplay(); });
carrusel.addEventListener("mouseenter", detenerAutoplay);
carrusel.addEventListener("mouseleave", arrancarAutoplay);
carrusel.addEventListener("focusin", detenerAutoplay);
carrusel.addEventListener("focusout", arrancarAutoplay);

// Deslizamiento táctil
let inicioX = null;
carrusel.addEventListener("pointerdown", (ev) => { inicioX = ev.clientX; });
carrusel.addEventListener("pointerup", (ev) => {
  if (inicioX === null) return;
  const delta = ev.clientX - inicioX;
  inicioX = null;
  if (Math.abs(delta) < 40) return;
  irADiapositiva(diapositivaActual + (delta < 0 ? 1 : -1));
  reiniciarAutoplay();
});

irADiapositiva(0);
arrancarAutoplay();

document.querySelectorAll(".accion-catalogo").forEach((btn) => {
  btn.addEventListener("click", () => {
    if (!document.querySelector("#panelCarrito").hidden) cerrarCapa("panelCarrito");
    document.querySelector("#catalogo").scrollIntoView({ behavior: movimientoReducido ? "auto" : "smooth" });
  });
});

// Sombra de la barra de categorías cuando queda pegada bajo la cabecera
const navCategorias = document.querySelector(".nav-categorias");
const centinelaNav = document.createElement("div");
navCategorias.before(centinelaNav);
new IntersectionObserver(([entrada]) => {
  navCategorias.classList.toggle("pegada", !entrada.isIntersecting);
}, { rootMargin: "-64px 0px 0px 0px" }).observe(centinelaNav);

// ---------- Catálogo ----------
const seccionesCatalogo = document.querySelector("#seccionesCatalogo");
const estadoCatalogo = document.querySelector("#estadoCatalogo");
const chipsCategorias = document.querySelector("#chipsCategorias");
const plantillaProducto = document.querySelector("#plantillaProducto");

function ilustracionCategoria(nombre) {
  const n = (nombre || "").toLowerCase();
  if (n.includes("pizza")) return "/img/tienda/pizza.svg";
  if (n.includes("combo") || n.includes("promo")) return "/img/tienda/combo.svg";
  if (n.includes("bebida") || n.includes("gaseosa") || n.includes("refresco") || n.includes("jugo")) return "/img/tienda/bebida.svg";
  if (n.includes("postre") || n.includes("dulce") || n.includes("helado")) return "/img/tienda/postre.svg";
  if (n.includes("adicional") || n.includes("complemento") || n.includes("entrada") || n.includes("extra")) return "/img/tienda/adicional.svg";
  return "/img/tienda/generico.svg";
}

// Convierte un enlace "compartir" de Google Drive a uno embebible como <img src>
function resolverUrlImagen(url) {
  const compartido = /drive\.google\.com\/file\/d\/([^/]+)/.exec(url);
  if (compartido) return `https://drive.google.com/thumbnail?id=${compartido[1]}&sz=w800`;
  return url;
}

// Cargador reutilizable "hámster en la rueda": clona la plantilla y ajusta su etiqueta accesible
const plantillaRuedaHamster = document.querySelector("#plantillaRuedaHamster");
function ruedaHamster(etiqueta) {
  const nodo = plantillaRuedaHamster.content.firstElementChild.cloneNode(true);
  if (etiqueta) nodo.setAttribute("aria-label", etiqueta);
  return nodo;
}

const TARJETA_ESQUELETO = '<div class="tarjeta-esqueleto" aria-hidden="true">'
  + '<div class="brillo esqueleto-arte"></div><div class="brillo esqueleto-linea"></div>'
  + '<div class="brillo esqueleto-linea corta"></div><div class="brillo esqueleto-boton"></div></div>';

// Estado de carga del catálogo: hámster + texto, y debajo el esqueleto de tarjetas
function pintarCargandoCatalogo() {
  estadoCatalogo.replaceChildren();
  const cargando = document.createElement("div");
  cargando.className = "cargando-hamster";
  cargando.append(ruedaHamster("Cargando el menú"));
  const texto = document.createElement("p");
  texto.className = "texto-cargando";
  texto.textContent = "Cargando el menú...";
  cargando.append(texto);
  const rejilla = document.createElement("div");
  rejilla.className = "rejilla-productos rejilla-esqueleto";
  rejilla.innerHTML = TARJETA_ESQUELETO.repeat(8);
  estadoCatalogo.append(cargando, rejilla);
}

async function cargarCatalogo() {
  estadoCatalogo.hidden = false;
  estadoCatalogo.classList.add("cargando");
  pintarCargandoCatalogo();
  try {
    catalogo = await peticion("GET", API.catalogo);
    // Descarta del carrito productos que ya no existen en el catálogo
    [...carrito.keys()].forEach((id) => {
      if (!catalogo.productos.some((p) => p.id === id)) carrito.delete(id);
    });
    persistirCarrito();
    estadoCatalogo.hidden = true;
    estadoCatalogo.classList.remove("cargando");
    pintarChips();
    pintarSecciones();
    pintarHeroPromos();
    actualizarIndicadores();
  } catch (err) {
    estadoCatalogo.classList.remove("cargando");
    estadoCatalogo.innerHTML = "";
    const mensaje = document.createElement("p");
    mensaje.textContent = `No pudimos cargar el menú. ${err.message}`;
    const reintentar = document.createElement("button");
    reintentar.type = "button";
    reintentar.className = "boton boton-primario";
    reintentar.textContent = "Reintentar";
    reintentar.addEventListener("click", cargarCatalogo);
    estadoCatalogo.append(mensaje, reintentar);
  }
}

function pintarChips() {
  chipsCategorias.querySelectorAll(".chip:not([data-categoria=''])").forEach((chip) => chip.remove());
  catalogo.categorias.forEach((cat) => {
    const chip = document.createElement("button");
    chip.type = "button";
    chip.className = "chip";
    chip.dataset.categoria = String(cat.id);
    chip.textContent = cat.nombre;
    chipsCategorias.append(chip);
  });
  chipsCategorias.querySelectorAll(".chip").forEach((chip) => {
    chip.addEventListener("click", () => {
      categoriaActiva = chip.dataset.categoria;
      chipsCategorias.querySelectorAll(".chip").forEach((c) => c.classList.toggle("activa", c === chip));
      aplicarFiltros();
      document.querySelector("#catalogo").scrollIntoView({ behavior: movimientoReducido ? "auto" : "smooth", block: "start" });
    });
  });
}

function pintarSecciones() {
  seccionesCatalogo.replaceChildren();

  if (catalogo.promociones?.length) {
    const promos = document.createElement("div");
    promos.className = "promos-activas";
    catalogo.promociones.forEach((promo) => {
      const etiqueta = document.createElement("span");
      etiqueta.className = "promo-etiqueta";
      etiqueta.textContent = promo.descripcion;
      promos.append(etiqueta);
    });
    seccionesCatalogo.append(promos);
  }

  catalogo.categorias.forEach((cat) => {
    const productos = catalogo.productos.filter((p) => p.idCategoria === cat.id);
    if (!productos.length) return;

    const seccion = document.createElement("section");
    seccion.className = "seccion-categoria";
    seccion.dataset.categoria = String(cat.id);
    seccion.id = `categoria-${cat.id}`;

    const titulo = document.createElement("h2");
    titulo.textContent = cat.nombre;
    const rejilla = document.createElement("div");
    rejilla.className = "rejilla-productos";
    productos.forEach((producto) => rejilla.append(crearTarjetaProducto(producto, cat.nombre)));
    seccion.append(titulo, rejilla);
    seccionesCatalogo.append(seccion);
  });

  const vacio = document.createElement("div");
  vacio.className = "sin-resultados";
  vacio.id = "sinResultados";
  vacio.hidden = true;
  const texto = document.createElement("p");
  texto.textContent = "No encontramos productos con esa búsqueda.";
  const limpiar = document.createElement("button");
  limpiar.type = "button";
  limpiar.className = "boton boton-secundario";
  limpiar.textContent = "Limpiar búsqueda";
  limpiar.addEventListener("click", () => {
    document.querySelectorAll("#buscador, #buscadorMovil").forEach((c) => (c.value = ""));
    termino = "";
    aplicarFiltros();
  });
  vacio.append(texto, limpiar);
  seccionesCatalogo.append(vacio);
  aplicarFiltros();
  activarScrollSpy();
}

// ---------- Scroll-spy: los chips reflejan la categoría visible (solo sin filtro activo) ----------
let observadorSpy = null;
const seccionesVisibles = new Set();

function resaltarChip(idCategoria) {
  chipsCategorias.querySelectorAll(".chip").forEach((chip) => {
    chip.classList.toggle("activa", chip.dataset.categoria === idCategoria);
  });
  const chip = chipsCategorias.querySelector(`.chip[data-categoria="${idCategoria}"]`);
  if (chip) chipsCategorias.scrollTo({ left: chip.offsetLeft - 16, behavior: movimientoReducido ? "auto" : "smooth" });
}

function activarScrollSpy() {
  observadorSpy?.disconnect();
  seccionesVisibles.clear();
  const secciones = [...seccionesCatalogo.querySelectorAll(".seccion-categoria")];
  if (!secciones.length) return;
  observadorSpy = new IntersectionObserver((entradas) => {
    entradas.forEach((e) => (e.isIntersecting ? seccionesVisibles.add(e.target) : seccionesVisibles.delete(e.target)));
    if (categoriaActiva) return; // con un filtro activo mandan los clics
    const visibles = secciones.filter((s) => seccionesVisibles.has(s));
    resaltarChip(visibles.length ? visibles[0].dataset.categoria : "");
  }, { rootMargin: "-130px 0px -55% 0px" });
  secciones.forEach((s) => observadorSpy.observe(s));
}

function crearTarjetaProducto(producto, nombreCategoria) {
  const tarjeta = plantillaProducto.content.firstElementChild.cloneNode(true);
  tarjeta.dataset.id = String(producto.id);
  tarjeta.dataset.nombre = producto.nombre.toLowerCase();
  const imagen = tarjeta.querySelector("img");
  const ilustracion = ilustracionCategoria(nombreCategoria);
  if (producto.imagenUrl) {
    imagen.src = resolverUrlImagen(producto.imagenUrl);
    imagen.onerror = () => { imagen.onerror = null; imagen.src = ilustracion; };
  } else {
    imagen.src = ilustracion;
  }
  tarjeta.querySelector(".producto-nombre").textContent = producto.nombre;
  tarjeta.querySelector(".producto-categoria").textContent = nombreCategoria || "";
  tarjeta.querySelector(".producto-precio").textContent = soles(producto.precio);

  // Insignias derivadas del propio catálogo (sin datos extra en BD)
  const insignia = tarjeta.querySelector(".insignia-producto");
  const nombre = producto.nombre.toLowerCase();
  const categoria = (nombreCategoria || "").toLowerCase();
  if (/diavola|picante|hot/.test(nombre)) {
    insignia.textContent = "Picante";
    insignia.classList.add("insignia-picante");
    insignia.hidden = false;
  } else if (/combo|promo/.test(categoria) || /combo/.test(nombre)) {
    insignia.textContent = "Combo";
    insignia.classList.add("insignia-combo");
    insignia.hidden = false;
  }

  tarjeta.querySelector(".boton-agregar").addEventListener("click", () => { cambiarCantidad(producto.id, 1); volarAlCarrito(tarjeta); });
  tarjeta.querySelector(".mas").addEventListener("click", () => cambiarCantidad(producto.id, 1));
  tarjeta.querySelector(".menos").addEventListener("click", () => cambiarCantidad(producto.id, -1));
  sincronizarTarjeta(tarjeta, producto.id);
  return tarjeta;
}

// Vuelo del producto al icono del carrito (transform/opacity; se omite con movimiento reducido)
function volarAlCarrito(tarjeta) {
  const latir = () => {
    contadorCarrito.classList.remove("late");
    void contadorCarrito.offsetWidth;
    contadorCarrito.classList.add("late");
  };
  if (movimientoReducido) { latir(); return; }
  const imagen = tarjeta.querySelector("img");
  const destino = document.querySelector("#btnCarrito");
  if (!imagen || !destino) return;
  const de = imagen.getBoundingClientRect();
  const a = destino.getBoundingClientRect();
  const clon = imagen.cloneNode();
  clon.className = "vuelo-carrito";
  clon.style.left = `${de.left + de.width / 2 - 24}px`;
  clon.style.top = `${de.top + de.height / 2 - 24}px`;
  document.body.append(clon);
  requestAnimationFrame(() => {
    const dx = (a.left + a.width / 2) - (de.left + de.width / 2);
    const dy = (a.top + a.height / 2) - (de.top + de.height / 2);
    clon.style.transform = `translate(${dx}px, ${dy}px) scale(0.25)`;
    clon.style.opacity = "0.25";
  });
  setTimeout(() => { clon.remove(); latir(); }, 560);
}

function sincronizarTarjeta(tarjeta, idProducto) {
  const cantidad = carrito.get(idProducto) || 0;
  tarjeta.querySelector(".boton-agregar").hidden = cantidad > 0;
  tarjeta.querySelector(".paso-cantidad").hidden = cantidad === 0;
  tarjeta.querySelector(".cantidad").textContent = cantidad;
}

function cambiarCantidad(idProducto, delta) {
  const nueva = (carrito.get(idProducto) || 0) + delta;
  if (nueva <= 0) carrito.delete(idProducto);
  else carrito.set(idProducto, Math.min(99, nueva));
  persistirCarrito();
  actualizarIndicadores();

  const tarjeta = seccionesCatalogo.querySelector(`.tarjeta-producto[data-id="${idProducto}"]`);
  if (tarjeta) sincronizarTarjeta(tarjeta, idProducto);

  cotizacion = null; // el total anterior ya no es válido
  if (!document.querySelector("#panelCarrito").hidden) {
    pintarCarrito();
    cotizarConRebote();
  }
}

function aplicarFiltros() {
  const buscar = termino.trim().toLowerCase();
  let visibles = 0;
  seccionesCatalogo.querySelectorAll(".seccion-categoria").forEach((seccion) => {
    const coincideCategoria = !categoriaActiva || seccion.dataset.categoria === categoriaActiva;
    let visiblesSeccion = 0;
    seccion.querySelectorAll(".tarjeta-producto").forEach((tarjeta) => {
      const visible = coincideCategoria && (!buscar || tarjeta.dataset.nombre.includes(buscar));
      tarjeta.hidden = !visible;
      if (visible) visiblesSeccion++;
    });
    seccion.hidden = visiblesSeccion === 0;
    visibles += visiblesSeccion;
  });
  const sinResultados = document.querySelector("#sinResultados");
  if (sinResultados) sinResultados.hidden = visibles > 0;
}

const buscarConRebote = rebotar((valor) => {
  termino = valor;
  aplicarFiltros();
}, 250);

document.querySelectorAll("#buscador, #buscadorMovil").forEach((campo) => {
  campo.addEventListener("input", () => buscarConRebote(campo.value));
});

// ---------- Carrito ----------
const panelCarrito = document.querySelector("#panelCarrito");
const lineasCarrito = document.querySelector("#lineasCarrito");
const carritoVacio = document.querySelector("#carritoVacio");
const piePanelCarrito = document.querySelector("#piePanelCarrito");
const notaCotizacion = document.querySelector("#notaCotizacion");
const totalCarritoNodo = document.querySelector("#totalCarrito");
const filaDescuento = document.querySelector("#filaDescuento");
const descuentoCarritoNodo = document.querySelector("#descuentoCarrito");
const btnPagar = document.querySelector("#btnPagar");
const plantillaLineaCarrito = document.querySelector("#plantillaLineaCarrito");

document.querySelector("#btnCarrito").addEventListener("click", abrirCarrito);
document.querySelector("#btnCerrarCarrito").addEventListener("click", () => cerrarCapa("panelCarrito"));

function abrirCarrito() {
  abrirCapa("panelCarrito", "fondoCarrito");
  pintarCarrito();
  if (carrito.size) cotizarCarrito();
}

function itemsDelCarrito() {
  return [...carrito.entries()].map(([idProducto, cantidad]) => ({ idProducto, cantidad }));
}

function pintarCarrito() {
  const vacio = carrito.size === 0;
  carritoVacio.hidden = !vacio;
  piePanelCarrito.hidden = vacio;
  lineasCarrito.replaceChildren();
  if (vacio) return;

  let totalReferencial = 0;
  carrito.forEach((cantidad, id) => {
    const producto = catalogo.productos.find((p) => p.id === id);
    if (!producto) return;
    const linea = plantillaLineaCarrito.content.firstElementChild.cloneNode(true);
    linea.dataset.id = String(id);
    linea.querySelector(".linea-nombre").textContent = producto.nombre;
    linea.querySelector(".linea-unitario").textContent = `${soles(producto.precio)} c/u`;
    linea.querySelector(".cantidad").textContent = cantidad;
    linea.querySelector(".linea-subtotal").textContent = soles(producto.precio * cantidad);
    linea.querySelector(".mas").addEventListener("click", () => cambiarCantidad(id, 1));
    linea.querySelector(".menos").addEventListener("click", () => cambiarCantidad(id, -1));
    linea.querySelector(".quitar").addEventListener("click", () => cambiarCantidad(id, -cantidad));
    lineasCarrito.append(linea);
    totalReferencial += producto.precio * cantidad;
  });

  // Mientras el servidor no confirme, el total es solo referencia y no se puede pagar
  filaDescuento.hidden = true;
  totalCarritoNodo.textContent = soles(totalReferencial);
  notaCotizacion.className = "nota-cotizacion";
  notaCotizacion.textContent = "Calculando total con promociones...";
  btnPagar.disabled = true;
}

const cotizarConRebote = rebotar(cotizarCarrito, 400);

async function cotizarCarrito() {
  if (!carrito.size) return;
  const secuencia = ++secuenciaCotizacion;
  try {
    const cuerpo = { items: itemsDelCarrito() };
    if (cuponCodigo) cuerpo.codigo = cuponCodigo;
    const respuesta = await peticion("POST", API.cotizar, cuerpo);
    if (secuencia !== secuenciaCotizacion) return; // llegó una respuesta vieja
    cotizacion = respuesta;
    aplicarCotizacion();
  } catch (err) {
    if (secuencia !== secuenciaCotizacion) return;
    cotizacion = null;
    notaCotizacion.className = "nota-cotizacion error";
    notaCotizacion.replaceChildren();
    notaCotizacion.append(`No pudimos confirmar el total. ${err.message} `);
    const reintentar = document.createElement("button");
    reintentar.type = "button";
    reintentar.className = "boton boton-fantasma";
    reintentar.style.minHeight = "32px";
    reintentar.textContent = "Reintentar";
    reintentar.addEventListener("click", cotizarCarrito);
    notaCotizacion.append(reintentar);
    btnPagar.disabled = true;
  }
}

function aplicarCotizacion() {
  let descuentoTotal = 0;
  cotizacion.lineas.forEach((lineaServidor) => {
    descuentoTotal += Number(lineaServidor.descuento || 0);
    const nodo = lineasCarrito.querySelector(`.linea-carrito[data-id="${lineaServidor.idProducto}"]`);
    if (!nodo) return;
    nodo.querySelector(".linea-subtotal").textContent = soles(lineaServidor.subtotal);
    const notaDescuento = nodo.querySelector(".linea-descuento");
    if (Number(lineaServidor.descuento) > 0) {
      notaDescuento.textContent = `Promo aplicada: -${soles(lineaServidor.descuento)}`;
      notaDescuento.hidden = false;
    } else {
      notaDescuento.hidden = true;
    }
  });
  filaDescuento.hidden = descuentoTotal <= 0;
  descuentoCarritoNodo.textContent = `- ${soles(descuentoTotal)}`;
  totalCarritoNodo.textContent = soles(cotizacion.total);
  notaCotizacion.className = "nota-cotizacion exito";
  notaCotizacion.textContent = "Total confirmado con promociones incluidas.";
  btnPagar.disabled = false;
}

// ---------- Cuenta: el ingreso/registro vive en su propia página (/tienda/ingresar) ----------
document.querySelectorAll(".alternar-clave").forEach((btn) => {
  btn.addEventListener("click", () => {
    const campo = btn.parentElement.querySelector("input");
    const mostrar = campo.type === "password";
    campo.type = mostrar ? "text" : "password";
    btn.setAttribute("aria-pressed", String(mostrar));
    btn.setAttribute("aria-label", mostrar ? "Ocultar contraseña" : "Mostrar contraseña");
  });
});

function limpiarErrores(formulario) {
  formulario.querySelectorAll(".error-campo, .error-general").forEach((nodo) => {
    nodo.textContent = "";
    nodo.classList.remove("visible");
  });
  formulario.querySelectorAll("[aria-invalid]").forEach((campo) => campo.removeAttribute("aria-invalid"));
}

function pintarErrores(formulario, campos = {}, general = "") {
  Object.entries(campos).forEach(([campo, mensaje]) => {
    const nodo = formulario.querySelector(`[data-error="${campo}"]`);
    const entrada = formulario.querySelector(`[data-campo="${campo}"]`);
    if (nodo) { nodo.textContent = mensaje; nodo.classList.add("visible"); }
    entrada?.setAttribute("aria-invalid", "true");
  });
  if (general) {
    const nodo = formulario.querySelector('[data-error="general"]');
    if (nodo) { nodo.textContent = general; nodo.classList.add("visible"); }
  }
}

// ---------- Checkout ----------
const pasoEntrega = document.querySelector("#pasoEntrega");
const pasoPago = document.querySelector("#pasoPago");
const pasoConfirmacion = document.querySelector("#pasoConfirmacion");
const lineasPago = document.querySelector("#lineasPago");
const resumenPagos = document.querySelector("#resumenPagos");
const btnConfirmarPedido = document.querySelector("#btnConfirmarPedido");
const plantillaLineaPago = document.querySelector("#plantillaLineaPago");
const MARCAS = { VISA: "#marca-visa", MASTERCARD: "#marca-mastercard" };

// ---------- Capa de envío (hámster) ----------
const capaEnviando = document.querySelector("#capaEnviando");
const textoEnviando = document.querySelector("#textoEnviando");
capaEnviando.prepend(ruedaHamster("Preparando tu pedido"));

const MENSAJES_ENVIO = [
  "Enviando tu pedido a la cocina...",
  "Preparando el horno...",
  "Amasando la masa madre...",
  "Estirando la pizza...",
];
const MINIMO_ENVIO = 7000; // el hámster corre al menos 7 s aunque el servidor responda antes
let rotadorEnvio = null;
const esperar = (ms) => new Promise((r) => setTimeout(r, ms));

function iniciarEnvio() {
  capaEnviando.hidden = false;
  let i = 0;
  textoEnviando.textContent = MENSAJES_ENVIO[0];
  // Con movimiento reducido el hámster queda estático; el texto igual se lee
  if (!movimientoReducido) {
    rotadorEnvio = setInterval(() => {
      i = (i + 1) % MENSAJES_ENVIO.length;
      textoEnviando.textContent = MENSAJES_ENVIO[i];
    }, 1800);
  }
}

function detenerEnvio() {
  capaEnviando.hidden = true;
  clearInterval(rotadorEnvio);
  rotadorEnvio = null;
}

btnPagar.addEventListener("click", async () => {
  cerrarCapa("panelCarrito");
  if (!sesion) {
    mostrarAviso("Inicia sesión para completar tu pedido.");
    location.href = "/tienda/ingresar?volver=pago";
    return;
  }
  await abrirPago();
});

async function abrirPago() {
  if (!carrito.size) { mostrarAviso("Tu carrito está vacío.", "error"); return; }
  if (!cotizacion) {
    await cotizarCarrito();
    if (!cotizacion) { mostrarAviso("No pudimos confirmar el total con el servidor.", "error"); abrirCarrito(); return; }
  }
  try {
    const cuenta = await peticion("GET", API.cuenta, undefined, true);
    tarjetasGuardadas = cuenta?.tarjetas || [];
  } catch (err) {
    if (err.status === 401) return; // ya se abrió el modal de sesión
    tarjetasGuardadas = [];
  }
  mostrarPaso(1);
  pintarModalidad();
  prepararLineasPago();
  abrirCapa("modalPago", "fondoPago");
}

function mostrarPaso(numero) {
  pasoEntrega.hidden = numero !== 1;
  pasoPago.hidden = numero !== 2;
  pasoConfirmacion.hidden = numero !== 3;
  document.querySelectorAll("#indicadorPasos .paso-punto").forEach((punto) => {
    const paso = Number(punto.dataset.paso);
    // En el paso 3 el pedido ya está confirmado: el último círculo también se marca hecho
    const completado = paso < numero || (numero === 3 && paso === 3);
    punto.classList.toggle("activo", paso === numero && !completado);
    punto.classList.toggle("completado", completado);
  });
}

document.querySelector("#btnIrAPago").addEventListener("click", () => {
  const campoDireccion = document.querySelector("#campoDireccion");
  const errorDireccion = document.querySelector('[data-error="direccion"]');
  errorDireccion.classList.remove("visible");
  campoDireccion.removeAttribute("aria-invalid");
  if (modalidad === "DELIVERY" && campoDireccion.value.trim().length < 8) {
    errorDireccion.textContent = "Ingresa una dirección completa (calle, número y distrito).";
    errorDireccion.classList.add("visible");
    campoDireccion.setAttribute("aria-invalid", "true");
    campoDireccion.focus();
    return;
  }
  pintarResumenEnvio();
  pintarResumenPedido();
  actualizarDesglose();
  mostrarPaso(2);
  recalcularResumenPagos();
});

document.querySelector("#btnVolverEntrega").addEventListener("click", () => mostrarPaso(1));

function pintarResumenEnvio() {
  const esDelivery = modalidad === "DELIVERY";
  document.querySelector("#resumenEnvioModalidad").textContent = esDelivery ? "Delivery a domicilio" : "Recojo en tienda";
  document.querySelector("#resumenEnvioDireccion").textContent = esDelivery
    ? document.querySelector("#campoDireccion").value.trim()
    : "Av. Los Olivos 123 - Lima";
}

// Resumen del pedido en el checkout: líneas e importes tal como los cotizó el servidor
function pintarResumenPedido() {
  const lista = document.querySelector("#resumenPedido");
  lista.replaceChildren();
  (cotizacion?.lineas || []).forEach((linea) => {
    const item = document.createElement("li");
    const nombre = document.createElement("span");
    nombre.className = "resumen-item";
    nombre.textContent = `${linea.cantidad}× ${linea.descripcion}`;
    const importe = document.createElement("span");
    importe.className = "resumen-importe";
    importe.textContent = soles(linea.subtotal);
    item.append(nombre, importe);
    if (Number(linea.descuento) > 0) {
      const descuento = document.createElement("span");
      descuento.className = "resumen-descuento";
      descuento.textContent = `Promo aplicada: -${soles(linea.descuento)}`;
      item.append(descuento);
    }
    lista.append(item);
  });
}

/* Desglose que SIEMPRE cuadra: precios con IGV incluido.
   El subtotal es la suma de líneas a precio lista (con IGV), el descuento se resta,
   y Subtotal - Descuentos = Total. El IGV no se suma: es informativo (ya va dentro
   del total, se extrae con total x 18/118). */
function actualizarDesglose() {
  const total = cotizacion.total;
  const ahorro = (cotizacion.lineas || []).reduce((suma, l) => suma + Number(l.descuento || 0), 0);
  const subtotal = total + ahorro;          // lo que costarían los productos sin promo (IGV incluido)
  const igv = total - total / 1.18;          // IGV contenido en lo que realmente se paga
  document.querySelector("#desgloseSubtotal").textContent = soles(subtotal);
  document.querySelector("#desgloseIgv").textContent = soles(igv);
  document.querySelector("#desgloseFilaAhorro").hidden = ahorro <= 0;
  document.querySelector("#desgloseAhorro").textContent = `- ${soles(ahorro)}`;
  document.querySelector("#desgloseFilaDelivery").hidden = modalidad !== "DELIVERY";
  document.querySelector("#desgloseTotal").textContent = soles(total);
  document.querySelector("#precioFooter").textContent = soles(total);
}

// Re-cotiza (con el cupón vigente) y refresca todo el paso 2 con el total del servidor
async function cotizarCheckout() {
  await cotizarCarrito();
  if (!cotizacion) throw new Error("No pudimos confirmar el total con el servidor.");
  pintarResumenPedido();
  actualizarDesglose();
  const lineas = lineasPago.querySelectorAll(".linea-pago");
  if (lineas.length === 1) lineas[0].querySelector(".monto-pago").value = Number(cotizacion.total).toFixed(2);
  recalcularResumenPagos();
}

// ---------- Cupón: se guarda el código y SIEMPRE se re-cotiza en servidor ----------
function pintarFeedbackPromo(tipo, mensaje) {
  const feedback = document.querySelector("#feedbackPromo");
  feedback.className = `feedback-promo visible ${tipo}`;
  feedback.replaceChildren();
  if (tipo === "exito") {
    const icono = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    icono.classList.add("icono");
    icono.innerHTML = '<use href="#icono-check"/>';
    feedback.append(icono);
  }
  feedback.append(document.createTextNode(mensaje));
}

document.querySelector("#btnAplicarPromo").addEventListener("click", async () => {
  const campo = document.querySelector("#campoCodigoPromo");
  const boton = document.querySelector("#btnAplicarPromo");
  const codigo = campo.value.trim();

  if (!codigo) {
    pintarFeedbackPromo("error", "Ingresa un código para aplicarlo.");
    campo.focus();
    return;
  }

  boton.disabled = true;
  document.querySelector("#feedbackPromo").className = "feedback-promo";
  const codigoPrevio = cuponCodigo;
  cuponCodigo = codigo;
  try {
    await cotizarCheckout();
    if (cotizacion.cuponAplicado === true) {
      pintarFeedbackPromo("exito", cotizacion.descripcionCupon || "Cupón aplicado a tu pedido.");
    } else if (cotizacion.cuponAplicado === false) {
      cuponCodigo = null;
      pintarFeedbackPromo("error", "Ese código no es válido o no aplica a tu pedido.");
    } else {
      // El servidor aún no soporta cupón en /cotizar: validación solo informativa
      const resultado = await peticion("POST", API.promoValidar, { codigo });
      pintarFeedbackPromo("exito", resultado.descripcion || "Código válido.");
    }
  } catch (err) {
    cuponCodigo = codigoPrevio;
    pintarFeedbackPromo("error", err.datos?.error || "Ese código no es válido.");
  } finally {
    boton.disabled = false;
  }
});

function prepararLineasPago() {
  lineasPago.replaceChildren();
  agregarLineaPago(cotizacion.total);
}

document.querySelector("#btnAgregarPago").addEventListener("click", () => {
  agregarLineaPago(Math.max(0, (centavos(cotizacion.total) - sumaPagosCentavos()) / 100));
});

function agregarLineaPago(montoInicial) {
  const linea = plantillaLineaPago.content.firstElementChild.cloneNode(true);
  const selectorMetodo = linea.querySelector(".selector-metodo");
  const montoInput = linea.querySelector(".monto-pago");
  const contenedorTarjetas = linea.querySelector(".tarjetas-guardadas");
  const bloqueTarjeta = linea.querySelector(".bloque-tarjeta");
  const tarjetaNueva = linea.querySelector(".tarjeta-nueva");
  const notaBilletera = linea.querySelector(".nota-billetera");
  const bloqueQr = linea.querySelector(".bloque-qr");
  const bloqueBilleteras = linea.querySelector(".bloque-billeteras");
  const imagenQr = linea.querySelector(".imagen-qr");
  const enlaceBilletera = linea.querySelector(".enlace-billetera");
  const numeroInput = linea.querySelector(".numero-tarjeta");
  const vencimientoInput = linea.querySelector(".vencimiento-tarjeta");
  const marcaNodo = linea.querySelector(".marca-detectada");

  montoInput.value = montoInicial > 0 ? montoInicial.toFixed(2) : "";

  // Tarjetas guardadas como flip-cards; si la cuenta no tiene ninguna, se muestra el formulario de tarjeta nueva
  construirTarjetasGuardadas(contenedorTarjetas, tarjetaNueva);

  const pintarMetodo = () => {
    const metodo = selectorMetodo.value;
    const esQr = metodo === "YAPE" || metodo === "PLIN";
    bloqueTarjeta.hidden = metodo !== "TARJETA";
    bloqueQr.hidden = !esQr;
    bloqueBilleteras.hidden = metodo !== "BILLETERA";
    notaBilletera.hidden = !(esQr || metodo === "BILLETERA");
    // Yape/Plin: mismo QR de marca que la página de instrucciones (se actualiza al cambiar de billetera)
    if (esQr) {
      const nombre = metodo === "YAPE" ? "Yape" : "Plin";
      imagenQr.src = `/tienda/pago-billetera/qr?metodo=${metodo.toLowerCase()}`;
      imagenQr.alt = `Código QR de demostración para pagar con ${nombre}`;
      enlaceBilletera.hidden = false;
      enlaceBilletera.href = `/tienda/pago-billetera?metodo=${metodo.toLowerCase()}`;
      enlaceBilletera.textContent = `Ver cómo pagar con ${nombre}`;
    } else {
      enlaceBilletera.hidden = true;
    }
  };
  selectorMetodo.addEventListener("change", pintarMetodo);
  pintarMetodo();

  // Selección de billetera simulada: solo resalta la elegida, el cobro no es real.
  bloqueBilleteras.querySelectorAll(".boton-billetera").forEach((boton) => {
    boton.addEventListener("click", () => {
      bloqueBilleteras.querySelectorAll(".boton-billetera").forEach((otro) =>
        otro.setAttribute("aria-pressed", String(otro === boton)));
    });
  });

  // Formato del número en bloques de 4 y detección de marca por BIN
  numeroInput.addEventListener("input", () => {
    const digitos = numeroInput.value.replace(/\D/g, "").slice(0, 16);
    numeroInput.value = digitos.replace(/(.{4})/g, "$1 ").trim();
    const marca = detectarMarca(digitos);
    marcaNodo.hidden = !marca;
    if (marca) marcaNodo.querySelector("use").setAttribute("href", MARCAS[marca]);
  });

  vencimientoInput.addEventListener("input", () => {
    let digitos = vencimientoInput.value.replace(/\D/g, "").slice(0, 4);
    if (digitos.length >= 3) digitos = `${digitos.slice(0, 2)}/${digitos.slice(2)}`;
    vencimientoInput.value = digitos;
  });

  linea.querySelector(".cvv-tarjeta").addEventListener("input", (ev) => {
    ev.target.value = ev.target.value.replace(/\D/g, "").slice(0, 4);
  });

  montoInput.addEventListener("input", recalcularResumenPagos);
  linea.querySelector(".usar-restante").addEventListener("click", () => {
    const otros = sumaPagosCentavos() - centavos(montoInput.value);
    montoInput.value = (Math.max(0, centavos(cotizacion.total) - otros) / 100).toFixed(2);
    recalcularResumenPagos();
  });
  linea.querySelector(".quitar-pago").addEventListener("click", () => {
    linea.remove();
    recalcularResumenPagos();
  });

  lineasPago.append(linea);
  recalcularResumenPagos();
}

// Normaliza la marca al logo del sprite (VISA / MASTERCARD / otra)
function claveMarca(marca) {
  const m = (marca || "").toUpperCase();
  if (m.includes("VISA")) return "VISA";
  if (m.includes("MASTER")) return "MASTERCARD";
  return "OTRA";
}

/* Radiogroup de tarjetas guardadas como flip-cards. La elección se guarda en
   dataset.seleccion ("" = tarjeta nueva) y la lee recogerPagos. Sin tarjetas
   guardadas se oculta y se muestra el formulario de tarjeta nueva (como antes). */
function construirTarjetasGuardadas(contenedor, tarjetaNueva) {
  if (!tarjetasGuardadas.length) {
    contenedor.hidden = true;
    contenedor.dataset.seleccion = "";
    tarjetaNueva.hidden = false;
    return;
  }
  contenedor.hidden = false;
  contenedor.replaceChildren();

  const opciones = [crearTileNueva(), ...tarjetasGuardadas.map(crearFlipCard)];
  opciones.forEach((op) => contenedor.append(op));

  const seleccionar = (elegido) => {
    opciones.forEach((op) => {
      const activo = op === elegido;
      op.setAttribute("aria-checked", String(activo));
      op.tabIndex = activo ? 0 : -1;
    });
    contenedor.dataset.seleccion = elegido.dataset.valor;
    tarjetaNueva.hidden = elegido.dataset.valor !== "";
  };

  opciones.forEach((op, i) => {
    op.addEventListener("click", () => seleccionar(op));
    op.addEventListener("keydown", (ev) => {
      if (ev.key === " " || ev.key === "Enter") { ev.preventDefault(); seleccionar(op); return; }
      if (["ArrowRight", "ArrowDown", "ArrowLeft", "ArrowUp"].includes(ev.key)) {
        ev.preventDefault();
        const avanza = ev.key === "ArrowRight" || ev.key === "ArrowDown";
        const siguiente = opciones[(i + (avanza ? 1 : -1) + opciones.length) % opciones.length];
        seleccionar(siguiente);
        siguiente.focus();
      }
    });
  });

  seleccionar(opciones[0]); // por defecto: tarjeta nueva
}

function crearTileNueva() {
  const tile = document.createElement("div");
  tile.className = "tarjeta-flip tarjeta-flip-nueva";
  tile.setAttribute("role", "radio");
  tile.setAttribute("aria-checked", "false");
  tile.dataset.valor = "";
  tile.tabIndex = -1;
  tile.setAttribute("aria-label", "Usar una tarjeta nueva");
  tile.innerHTML = '<svg class="icono" aria-hidden="true"><use href="#icono-mas"/></svg><span>Nueva tarjeta</span>';
  return tile;
}

function crearFlipCard(tarjeta) {
  const clave = claveMarca(tarjeta.marca);
  const tile = document.createElement("div");
  tile.className = `tarjeta-flip tarjeta-flip-${clave.toLowerCase()}`;
  tile.setAttribute("role", "radio");
  tile.setAttribute("aria-checked", "false");
  tile.dataset.valor = String(tarjeta.id);
  tile.tabIndex = -1;
  tile.setAttribute("aria-label", `${tarjeta.marca} terminada en ${tarjeta.ultimos4}, titular ${tarjeta.titular}`);

  const logo = clave === "VISA" ? "#logo-visa" : clave === "MASTERCARD" ? "#logo-mastercard" : "";
  const marcaHtml = logo
    ? `<svg class="tarjeta-flip-logo" aria-hidden="true"><use href="${logo}"/></svg>`
    : '<span class="tarjeta-flip-marca-texto"></span>';

  tile.innerHTML =
    '<div class="tarjeta-flip-interior">'
    + '<div class="tarjeta-flip-cara tarjeta-flip-frente">'
    + `<div class="tarjeta-flip-cabecera">${marcaHtml}`
    + '<svg class="tarjeta-flip-contactless" aria-hidden="true" viewBox="0 0 24 24"><path d="M8 8a6 6 0 0 1 0 8M11 6a9 9 0 0 1 0 12M14 4a12 12 0 0 1 0 16" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>'
    + '</div>'
    + '<div class="tarjeta-flip-chip" aria-hidden="true"></div>'
    + '<div class="tarjeta-flip-numero"></div>'
    + '<div class="tarjeta-flip-titular"></div>'
    + '<svg class="tarjeta-flip-check" aria-hidden="true"><use href="#icono-check"/></svg>'
    + '</div>'
    + '<div class="tarjeta-flip-cara tarjeta-flip-dorso">'
    + '<div class="tarjeta-flip-banda" aria-hidden="true"></div>'
    + '<div class="tarjeta-flip-firma"><span>***</span></div>'
    + '</div>'
    + '</div>';

  // Datos del titular por textContent (nunca por innerHTML) y número siempre enmascarado
  tile.querySelector(".tarjeta-flip-numero").textContent = `•••• •••• •••• ${tarjeta.ultimos4}`;
  tile.querySelector(".tarjeta-flip-titular").textContent = tarjeta.titular;
  if (!logo) tile.querySelector(".tarjeta-flip-marca-texto").textContent = tarjeta.marca;
  return tile;
}

function detectarMarca(digitos) {
  if (/^4/.test(digitos)) return "VISA";
  const cuatro = Number(digitos.slice(0, 4));
  if (/^5[1-5]/.test(digitos) || (cuatro >= 2221 && cuatro <= 2720)) return "MASTERCARD";
  return null;
}

function esNumeroValido(digitos) {
  if (digitos.length < 13 || digitos.length > 16) return false;
  // Validación de Luhn
  let suma = 0;
  let doblar = false;
  for (let i = digitos.length - 1; i >= 0; i--) {
    let d = Number(digitos[i]);
    if (doblar) { d *= 2; if (d > 9) d -= 9; }
    suma += d;
    doblar = !doblar;
  }
  return suma % 10 === 0;
}

function esVencimientoValido(valor) {
  const partes = /^(\d{2})\/(\d{2})$/.exec(valor);
  if (!partes) return false;
  const mes = Number(partes[1]);
  if (mes < 1 || mes > 12) return false;
  const ahora = new Date();
  const anio = 2000 + Number(partes[2]);
  return anio > ahora.getFullYear() || (anio === ahora.getFullYear() && mes >= ahora.getMonth() + 1);
}

function sumaPagosCentavos() {
  let suma = 0;
  lineasPago.querySelectorAll(".monto-pago").forEach((campo) => { suma += centavos(campo.value); });
  return suma;
}

function recalcularResumenPagos() {
  const lineas = lineasPago.querySelectorAll(".linea-pago");
  lineas.forEach((linea) => {
    linea.querySelector(".quitar-pago").hidden = lineas.length <= 1;
  });
  if (!cotizacion) return;
  const diferencia = centavos(cotizacion.total) - sumaPagosCentavos();
  if (diferencia > 0) {
    resumenPagos.className = "resumen-pagos falta";
    resumenPagos.textContent = `Falta cubrir ${soles(diferencia / 100)}.`;
    btnConfirmarPedido.disabled = true;
  } else if (diferencia < 0) {
    resumenPagos.className = "resumen-pagos sobra";
    resumenPagos.textContent = `Los pagos superan el total por ${soles(-diferencia / 100)}.`;
    btnConfirmarPedido.disabled = true;
  } else {
    resumenPagos.className = "resumen-pagos cuadra";
    resumenPagos.textContent = "Los pagos cubren el total exacto.";
    btnConfirmarPedido.disabled = false;
  }
}

function recogerPagos() {
  const pagos = [];
  for (const linea of lineasPago.querySelectorAll(".linea-pago")) {
    limpiarErrores(linea);
    const metodo = linea.querySelector(".selector-metodo").value;
    const monto = Number(linea.querySelector(".monto-pago").value);
    if (!(monto > 0)) { mostrarAviso("Cada método necesita un monto mayor a cero.", "error"); return null; }
    const pago = { metodo, monto };

    if (metodo === "TARJETA") {
      const contenedorTarjetas = linea.querySelector(".tarjetas-guardadas");
      const idGuardada = contenedorTarjetas && !contenedorTarjetas.hidden ? (contenedorTarjetas.dataset.seleccion || "") : "";
      if (idGuardada) {
        pago.tarjeta = { idGuardada: Number(idGuardada) };
      } else {
        const numero = linea.querySelector(".numero-tarjeta").value.replace(/\D/g, "");
        const titular = linea.querySelector(".titular-tarjeta").value.trim();
        const vencimiento = linea.querySelector(".vencimiento-tarjeta").value;
        const cvv = linea.querySelector(".cvv-tarjeta").value;
        const errores = {};
        if (!esNumeroValido(numero)) errores.numero = "Revisa el número de la tarjeta.";
        if (!titular) errores.titular = "Ingresa el nombre del titular.";
        if (!esVencimientoValido(vencimiento)) errores.vencimiento = "Usa MM/AA y una fecha vigente.";
        if (cvv.length < 3) errores.cvv = "CVV incompleto.";
        if (Object.keys(errores).length) { pintarErrores(linea, errores); return null; }
        // El CVV solo se valida en pantalla: nunca se guarda ni viaja al backend
        pago.tarjeta = {
          numero,
          titular,
          vencimiento,
          guardar: linea.querySelector(".guardar-tarjeta").checked,
        };
      }
    }
    pagos.push(pago);
  }
  return pagos;
}

btnConfirmarPedido.addEventListener("click", async () => {
  const pagos = recogerPagos();
  if (!pagos) return;
  const entrega = { modalidad };
  if (modalidad === "DELIVERY") entrega.direccion = document.querySelector("#campoDireccion").value.trim();

  btnConfirmarPedido.disabled = true;
  iniciarEnvio();
  try {
    // La confirmación solo aparece cuando responde el servidor Y pasa el mínimo del hámster.
    // Si el servidor falla, Promise.all rechaza al instante y el loader se oculta sin esperar.
    const [respuesta] = await Promise.all([
      peticion("POST", API.pedidos, { items: itemsDelCarrito(), entrega, pagos }, true),
      esperar(MINIMO_ENVIO),
    ]);
    detenerEnvio();
    document.querySelector("#numeroPedido").textContent = `N° ${respuesta.idPedido}`;
    document.querySelector("#detalleConfirmacion").textContent =
      `Total ${soles(respuesta.total)} · Estado: ${respuesta.estado}. ` +
      (modalidad === "DELIVERY" ? "Te avisaremos cuando salga el repartidor." : "Te avisaremos cuando esté listo para recoger.");
    carrito.clear();
    persistirCarrito();
    cotizacion = null;
    actualizarIndicadores();
    seccionesCatalogo.querySelectorAll(".tarjeta-producto").forEach((tarjeta) => sincronizarTarjeta(tarjeta, Number(tarjeta.dataset.id)));
    mostrarPaso(3);
  } catch (err) {
    detenerEnvio();
    if (err.status === 400 && err.datos?.campos) {
      pintarErrores(document.querySelector("#modalPago"), err.datos.campos, err.datos.error);
      mostrarAviso(err.datos.error || "Revisa los datos del pedido.", "error");
    } else if (err.status !== 401) {
      mostrarAviso(err.message, "error");
    }
  } finally {
    recalcularResumenPagos();
  }
});

document.querySelector("#btnSeguirComprando").addEventListener("click", () => {
  cerrarCapa("modalPago");
  document.querySelector("#catalogo").scrollIntoView({ behavior: movimientoReducido ? "auto" : "smooth" });
});

// ---------- Arranque ----------
cargarEstadoGuardado();
pintarSesion();
pintarModalidad();
actualizarIndicadores();
cargarCatalogo().then(() => {
  // Al volver de /tienda/ingresar con ?pago=1 se retoma el checkout pendiente
  const params = new URLSearchParams(location.search);
  if (params.get("pago") === "1") {
    history.replaceState(null, "", "/tienda");
    if (sesion && carrito.size) abrirPago();
  }
});
