/* Página de ingreso/registro de la tienda Mamma Tomato.
   Emite el JWT contra /api/tienda/auth y vuelve a la tienda; con ?volver=pago
   la tienda retoma el checkout pendiente. */

import { UBIGEO } from "./ubigeo.js";

const API = {
  registro: "/api/tienda/auth/registro",
  ingreso: "/api/tienda/auth/login",
  verificar: "/api/tienda/cuenta/verificar",
  reenviar: "/api/tienda/cuenta/reenviar",
};
const CLAVE_SESION = "tienda.sesion";

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

// El chain de /api/tienda es JWT stateless; las llamadas autenticadas viajan con
// Authorization: Bearer <token>. El CSRF se manda por compatibilidad si existe.
async function peticion(metodo, url, cuerpo, token) {
  const headers = { Accept: "application/json", "Content-Type": "application/json" };
  if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
  if (token) headers.Authorization = `Bearer ${token}`;
  let res;
  try {
    res = await fetch(url, { method: metodo, headers, body: JSON.stringify(cuerpo) });
  } catch {
    throw Object.assign(new Error("Sin conexión con el servidor. Revisa tu internet."), { deRed: true });
  }
  if (!res.ok) {
    let datos = null;
    try { datos = await res.json(); } catch { /* respuesta sin cuerpo */ }
    throw Object.assign(new Error(datos?.error || `Error ${res.status}`), { status: res.status, datos });
  }
  return res.json();
}

// ---------- Avisos ----------
const zonaAvisos = document.querySelector("#zonaAvisos");
function mostrarAviso(mensaje, tipo = "info") {
  const aviso = document.createElement("div");
  aviso.className = `aviso aviso-${tipo}`;
  aviso.textContent = mensaje;
  zonaAvisos.append(aviso);
  setTimeout(() => aviso.remove(), 3200);
}

// ---------- Pestañas ----------
const formularioIngreso = document.querySelector("#formularioIngreso");
const formularioRegistro = document.querySelector("#formularioRegistro");
const pestanaIngresar = document.querySelector("#pestanaIngresar");
const pestanaRegistrar = document.querySelector("#pestanaRegistrar");

function cambiarPestana(cual) {
  const ingreso = cual === "ingresar";
  pestanaIngresar.classList.toggle("activa", ingreso);
  pestanaRegistrar.classList.toggle("activa", !ingreso);
  pestanaIngresar.setAttribute("aria-selected", String(ingreso));
  pestanaRegistrar.setAttribute("aria-selected", String(!ingreso));
  formularioIngreso.hidden = !ingreso;
  formularioRegistro.hidden = ingreso;
}
pestanaIngresar.addEventListener("click", () => cambiarPestana("ingresar"));
pestanaRegistrar.addEventListener("click", () => cambiarPestana("registrar"));

document.querySelectorAll(".alternar-clave").forEach((btn) => {
  btn.addEventListener("click", () => {
    const campo = btn.parentElement.querySelector("input");
    const mostrar = campo.type === "password";
    campo.type = mostrar ? "text" : "password";
    btn.setAttribute("aria-pressed", String(mostrar));
    btn.setAttribute("aria-label", mostrar ? "Ocultar contraseña" : "Mostrar contraseña");
  });
});

// ---------- Fuerza de la contraseña (solo registro) ----------
// El backend exigirá la misma política; esto es validación en vivo de respaldo.
const REQUISITOS_CLAVE = [
  { prueba: (v) => v.length >= 8, texto: "Al menos 8 caracteres" },
  { prueba: (v) => /[A-Z]/.test(v), texto: "Una letra mayúscula" },
  { prueba: (v) => /[a-z]/.test(v), texto: "Una letra minúscula" },
  { prueba: (v) => /\d/.test(v), texto: "Un número" },
  { prueba: (v) => /[^A-Za-z0-9]/.test(v), texto: "Un carácter especial (! @ # $...)" },
];

const claveRegistro = formularioRegistro.querySelector('[data-campo="password"]');
const botonCrear = formularioRegistro.querySelector('[type="submit"]');
const alertaClave = document.querySelector("#alertaClave");
const alertaClaveLista = document.querySelector("#alertaClaveLista");

function claveEsSegura(valor) {
  return REQUISITOS_CLAVE.every((r) => r.prueba(valor));
}

function evaluarClave() {
  const valor = claveRegistro.value;
  const faltantes = REQUISITOS_CLAVE.filter((r) => !r.prueba(valor));
  const cumple = faltantes.length === 0;
  botonCrear.disabled = !cumple;

  // Campo vacío o contraseña completa: sin alerta. Con pendientes: se listan solo los que faltan.
  if (!valor || cumple) {
    alertaClave.hidden = true;
    alertaClaveLista.replaceChildren();
    return;
  }
  alertaClave.hidden = false;
  alertaClaveLista.replaceChildren(...faltantes.map((r) => {
    const li = document.createElement("li");
    li.textContent = r.texto;
    return li;
  }));
}

claveRegistro.addEventListener("input", evaluarClave);
evaluarClave(); // arranca deshabilitado hasta que la contraseña cumpla

// ---------- Combos de ubicación (departamento → provincia → distrito) ----------
const comboDepartamento = document.querySelector("#comboDepartamento");
const comboProvincia = document.querySelector("#comboProvincia");
const comboDistrito = document.querySelector("#comboDistrito");

function llenarCombo(combo, valores, marcador) {
  combo.replaceChildren(new Option(marcador, ""));
  valores.forEach((v) => combo.append(new Option(v, v)));
  combo.disabled = valores.length === 0;
}

llenarCombo(comboDepartamento, Object.keys(UBIGEO), "Selecciona");

comboDepartamento.addEventListener("change", () => {
  const provincias = UBIGEO[comboDepartamento.value] || {};
  llenarCombo(comboProvincia, Object.keys(provincias), "Selecciona");
  llenarCombo(comboDistrito, [], "Selecciona");
});

comboProvincia.addEventListener("change", () => {
  const distritos = (UBIGEO[comboDepartamento.value] || {})[comboProvincia.value] || [];
  llenarCombo(comboDistrito, distritos, "Selecciona");
});

// ---------- Errores de formulario ----------
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

function validarRegistro(datos) {
  const campos = {};
  if (!datos.nombres.trim()) campos.nombres = "Ingresa tus nombres.";
  if (!datos.apellidoPaterno.trim()) campos.apellidoPaterno = "Ingresa tu apellido paterno.";
  if (!datos.apellidoMaterno.trim()) campos.apellidoMaterno = "Ingresa tu apellido materno.";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(datos.email)) campos.email = "Ingresa un email válido.";
  if (!/^9\d{8}$/.test(datos.telefono)) campos.telefono = "El teléfono debe tener 9 dígitos y empezar en 9.";
  if (!claveEsSegura(datos.password || "")) campos.password = "Tu contraseña aún no cumple los requisitos de seguridad.";
  if (!datos.departamento) campos.departamento = "Selecciona el departamento.";
  if (!datos.provincia) campos.provincia = "Selecciona la provincia.";
  if (!datos.distrito) campos.distrito = "Selecciona el distrito.";
  if (!datos.direccionExacta.trim()) campos.direccionExacta = "Ingresa tu dirección exacta.";
  return campos;
}

// ---------- Sesión y retorno ----------
function guardarSesion(datos, recordar) {
  const sesion = JSON.stringify({ token: datos.token, nombre: datos.nombre, email: datos.email });
  // "Recordarme" decide si la sesión sobrevive al cierre del navegador
  localStorage.removeItem(CLAVE_SESION);
  sessionStorage.removeItem(CLAVE_SESION);
  (recordar ? localStorage : sessionStorage).setItem(CLAVE_SESION, sesion);
}

function volverALaTienda() {
  const volver = new URLSearchParams(location.search).get("volver");
  location.href = volver === "pago" ? "/tienda?pago=1" : "/tienda";
}

// ---------- Envíos ----------
formularioIngreso.addEventListener("submit", async (ev) => {
  ev.preventDefault();
  limpiarErrores(formularioIngreso);
  const datos = Object.fromEntries(new FormData(formularioIngreso));
  const boton = formularioIngreso.querySelector('[type="submit"]');
  boton.disabled = true;
  try {
    const sesion = await peticion("POST", API.ingreso, { email: datos.email, password: datos.password });
    guardarSesion(sesion, datos.recordar !== undefined);
    volverALaTienda();
  } catch (err) {
    if (err.status === 401) pintarErrores(formularioIngreso, {}, err.datos?.error || "Email o contraseña incorrectos.");
    else if (err.status === 400) pintarErrores(formularioIngreso, err.datos?.campos || {}, err.datos?.error);
    else mostrarAviso(err.message, "error");
    boton.disabled = false;
  }
});

formularioRegistro.addEventListener("submit", async (ev) => {
  ev.preventDefault();
  limpiarErrores(formularioRegistro);
  const datos = Object.fromEntries(new FormData(formularioRegistro));
  const invalidos = validarRegistro(datos);
  if (Object.keys(invalidos).length) {
    pintarErrores(formularioRegistro, invalidos);
    formularioRegistro.querySelector("[aria-invalid]")?.focus();
    return;
  }
  const boton = formularioRegistro.querySelector('[type="submit"]');
  boton.disabled = true;
  try {
    const sesion = await peticion("POST", API.registro, datos);
    guardarSesion(sesion, true); // cuenta nueva: se recuerda en este dispositivo
    // La cuenta ya quedó creada y con sesión; el correo se verifica en el paso siguiente.
    if (sesion.emailVerificado) volverALaTienda();
    else mostrarVerificacion(sesion);
  } catch (err) {
    if (err.status === 409) pintarErrores(formularioRegistro, { email: err.datos?.error || "Este email ya está registrado." });
    else if (err.status === 400) pintarErrores(formularioRegistro, err.datos?.campos || {}, err.datos?.error);
    else mostrarAviso(err.message, "error");
    boton.disabled = false;
  }
});

// ---------- Paso de verificación de correo (tras el registro) ----------
const pestanas = document.querySelector(".pestanas");
const pasoVerificacion = document.querySelector("#pasoVerificacion");
const verificacionEmail = document.querySelector("#verificacionEmail");
const verificacionContador = document.querySelector("#verificacionContador");
const verificacionTiempo = document.querySelector("#verificacionTiempo");
const campoCodigo = document.querySelector("#campoCodigo");
const errorCodigo = document.querySelector("#errorCodigo");
const botonVerificar = document.querySelector("#botonVerificar");
const botonReenviar = document.querySelector("#botonReenviar");
const enlaceVerificarLuego = document.querySelector("#enlaceVerificarLuego");

const DURACION_CODIGO = 600; // 10 minutos, igual que el backend
const ESPERA_REENVIO = 30;   // segundos antes de poder reenviar
let tokenVerificacion = null;
let idContador = null;
let segundosRestantes = 0;

function formatearTiempo(seg) {
  const s = Math.max(seg, 0);
  return `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
}

function detenerContador() {
  if (idContador) { clearInterval(idContador); idContador = null; }
}

function iniciarContador() {
  detenerContador();
  segundosRestantes = DURACION_CODIGO;
  verificacionContador.classList.remove("vencido");
  verificacionTiempo.textContent = formatearTiempo(segundosRestantes);
  botonReenviar.disabled = true;
  idContador = setInterval(() => {
    segundosRestantes -= 1;
    verificacionTiempo.textContent = formatearTiempo(segundosRestantes);
    // El reenvío se habilita tras una breve espera y sigue disponible al vencer
    if (segundosRestantes <= DURACION_CODIGO - ESPERA_REENVIO) botonReenviar.disabled = false;
    if (segundosRestantes <= 0) {
      detenerContador();
      verificacionContador.classList.add("vencido");
      botonReenviar.disabled = false;
    }
  }, 1000);
}

function limpiarErrorCodigo() {
  errorCodigo.textContent = "";
  errorCodigo.classList.remove("visible");
  campoCodigo.removeAttribute("aria-invalid");
}

function pintarErrorCodigo(mensaje) {
  errorCodigo.textContent = mensaje;
  errorCodigo.classList.add("visible");
  campoCodigo.setAttribute("aria-invalid", "true");
}

function mostrarVerificacion(sesion) {
  tokenVerificacion = sesion.token;
  verificacionEmail.textContent = sesion.email || "tu correo";
  pestanas.hidden = true;
  formularioIngreso.hidden = true;
  formularioRegistro.hidden = true;
  pasoVerificacion.hidden = false;
  limpiarErrorCodigo();
  campoCodigo.value = "";
  iniciarContador();
  campoCodigo.focus();
}

// Solo dígitos en el input del código
campoCodigo.addEventListener("input", () => {
  campoCodigo.value = campoCodigo.value.replace(/\D/g, "").slice(0, 6);
  limpiarErrorCodigo();
});
campoCodigo.addEventListener("keydown", (ev) => {
  if (ev.key === "Enter") { ev.preventDefault(); botonVerificar.click(); }
});

botonVerificar.addEventListener("click", async () => {
  const codigo = campoCodigo.value.trim();
  limpiarErrorCodigo();
  if (!/^\d{6}$/.test(codigo)) {
    pintarErrorCodigo("Ingresa el código de 6 dígitos.");
    campoCodigo.focus();
    return;
  }
  botonVerificar.disabled = true;
  try {
    await peticion("POST", API.verificar, { codigo }, tokenVerificacion);
    detenerContador();
    mostrarAviso("¡Correo verificado!", "exito");
    setTimeout(volverALaTienda, 700);
  } catch (err) {
    if (err.status === 400) pintarErrorCodigo(err.datos?.error || "El código es incorrecto o ha vencido.");
    else mostrarAviso(err.message, "error");
    botonVerificar.disabled = false;
    campoCodigo.focus();
  }
});

botonReenviar.addEventListener("click", async () => {
  botonReenviar.disabled = true;
  try {
    await peticion("POST", API.reenviar, {}, tokenVerificacion);
    limpiarErrorCodigo();
    campoCodigo.value = "";
    iniciarContador();
    mostrarAviso("Código reenviado", "exito");
    campoCodigo.focus();
  } catch (err) {
    mostrarAviso(err.message, "error");
    botonReenviar.disabled = false;
  }
});

// Verificación blanda: la cuenta ya existe y tiene sesión, se puede verificar luego
enlaceVerificarLuego.addEventListener("click", () => {
  detenerContador();
  volverALaTienda();
});

// Si ya hay sesión, no tiene sentido quedarse aquí
try {
  const sesion = JSON.parse(localStorage.getItem(CLAVE_SESION) || sessionStorage.getItem(CLAVE_SESION));
  if (sesion?.token) volverALaTienda();
} catch { /* sin sesión previa */ }
