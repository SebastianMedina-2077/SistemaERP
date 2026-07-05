const csrf = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
const money = (v) => `S/ ${Number(v || 0).toFixed(2)}`;

async function api(method, url, body) {
  const headers = { Accept: "application/json" };
  if (body) headers["Content-Type"] = "application/json";
  if (csrfHeader && csrf) headers[csrfHeader] = csrf;
  const res = await fetch(url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    const err = new Error(data.mensaje || res.statusText);
    err.status = res.status;
    err.body = data;
    throw err;
  }
  return res.json();
}

const aperturaModal = document.querySelector("#aperturaModal");
const aperturaMonto = document.querySelector("#aperturaMonto");
const aperturaConfirm = document.querySelector("#aperturaConfirm");

const btnCerrarCaja = document.querySelector("#btnCerrarCaja");
const cierreModal = document.querySelector("#cierreModal");
const cierreClose = document.querySelector("#cierreClose");
const cierreConfirm = document.querySelector("#cierreConfirm");
const cajaContado = document.querySelector("#cajaContado");
const cierreResultado = document.querySelector("#cierreResultado");
const rep = {
  inicial: document.querySelector("#repInicial"),
  efectivo: document.querySelector("#repEfectivo"),
  tarjeta: document.querySelector("#repTarjeta"),
  yape: document.querySelector("#repYape"),
  plin: document.querySelector("#repPlin"),
  total: document.querySelector("#repTotal"),
  esperado: document.querySelector("#repEsperado"),
};

const cajaLock = document.querySelector("#cajaLock");
const lockPin = document.querySelector("#lockPin");
const lockBtn = document.querySelector("#lockUnlock");
const lockError = document.querySelector("#cajaLockError");

async function init() {
  try {
    const estado = await api("GET", "/cajero/caja/estado");
    if (estado.bloqueada) mostrarLock();
    else if (!estado.abierta) mostrarApertura(estado.montoInicial);
  } catch {
    /* si el estado falla, no bloqueamos el POS */
  }
}

function mostrarApertura(monto) {
  aperturaMonto.value = Number(monto || 400).toFixed(2);
  aperturaModal.classList.remove("hidden");
  setTimeout(() => aperturaConfirm.focus(), 50);
}

aperturaConfirm.addEventListener("click", async () => {
  const monto = Number(aperturaMonto.value) || 400;
  aperturaConfirm.disabled = true;
  try {
    await api("POST", "/cajero/caja/abrir", { montoInicial: monto });
    aperturaModal.classList.add("hidden");
    MammaTomatoAlert.success("Caja abierta", `Monto inicial ${money(monto)}`);
  } catch (err) {
    MammaTomatoAlert.error("No se pudo abrir la caja", err.message);
  } finally {
    aperturaConfirm.disabled = false;
  }
});

async function abrirCierre() {
  try {
    // Revalida el estado real antes de abrir el cuadre: si el servidor se reinicio
    // (almacenamiento en memoria) la caja pudo perderse y hay que reabrirla.
    const estado = await api("GET", "/cajero/caja/estado");
    if (estado.bloqueada) { mostrarLock(); return; }
    if (!estado.abierta) { mostrarApertura(estado.montoInicial); return; }

    const r = await api("GET", "/cajero/caja/reporte");
    rep.inicial.textContent = money(r.montoInicial);
    rep.efectivo.textContent = money(r.efectivo);
    rep.tarjeta.textContent = money(r.tarjeta);
    rep.yape.textContent = money(r.yape);
    rep.plin.textContent = money(r.plin);
    rep.total.textContent = money(r.totalVentas);
    rep.esperado.textContent = money(r.efectivoEsperado);
    cajaContado.value = "";
    cajaContado.disabled = false;
    cierreConfirm.disabled = false;
    cierreResultado.className = "caja-resultado hidden";
    cierreResultado.innerHTML = "";
    cierreModal.classList.remove("hidden");
    setTimeout(() => cajaContado.focus(), 50);
  } catch (err) {
    if (err.body && err.body.sinCaja) { mostrarApertura(err.body.montoInicial); return; }
    MammaTomatoAlert.error("No se pudo abrir el cuadre", err.message);
  }
}

function cerrarCierre() {
  cierreModal.classList.add("hidden");
}

btnCerrarCaja.addEventListener("click", abrirCierre);
cierreClose.addEventListener("click", cerrarCierre);
cierreModal.addEventListener("click", (e) => { if (e.target === cierreModal) cerrarCierre(); });

cierreConfirm.addEventListener("click", () => enviarCuadre(false));

// Envia el cuadre al backend. confirmar=true solo en la segunda pasada de un sobrante.
async function enviarCuadre(confirmar) {
  if (cajaContado.value === "") {
    MammaTomatoAlert.warning("Falta el monto", "Ingresa el efectivo contado en caja");
    return;
  }
  const contado = Number(cajaContado.value) || 0;
  cierreConfirm.disabled = true;
  try {
    manejarResultado(await api("POST", "/cajero/caja/cuadre", { contado, confirmar }));
  } catch (err) {
    if (err.body && err.body.sinCaja) {
      cerrarCierre();
      mostrarApertura(err.body.montoInicial);
      MammaTomatoAlert.warning("Caja no abierta", "Vuelve a abrir la caja para registrar el turno.");
      return;
    }
    MammaTomatoAlert.error("Error en el cuadre", err.message);
    cierreConfirm.disabled = false;
  }
}

function manejarResultado(r) {
  const dif = Number(r.diferencia);
  if (r.resultado === "CUADRA") {
    mostrarCuadre("ok", r, "El efectivo coincide con lo esperado. Turno cerrado.");
    finalizarTurno();
  } else if (r.resultado === "SOBRA") {
    if (r.requiereConfirmacion) {
      // Sobrante: pedir una segunda confirmacion antes de cerrar el turno.
      mostrarCuadre("sobra", r, "Revisa el efectivo y confirma para cerrar el turno.");
      cierreConfirm.disabled = false;
      mostrarModalConfirmacion({
        titulo: "Hay un sobrante en caja",
        mensaje: `El efectivo contado supera lo esperado en ${money(dif)}. ¿Cerrar el turno de todas formas?`,
        labelConfirmar: "Cerrar turno",
        labelCancelar: "Volver a contar",
        onConfirmar: () => enviarCuadre(true),
      });
    } else {
      mostrarCuadre("sobra", r, "Se registra el sobrante. Turno cerrado.");
      finalizarTurno();
    }
  } else if (r.resultado === "FALTA") {
    mostrarCuadre("falta", r, `Vuelve a contar el efectivo. Intentos restantes: ${r.intentosRestantes}.`);
    cajaContado.value = "";
    cierreConfirm.disabled = false;
    cajaContado.focus();
  } else if (r.resultado === "BLOQUEADA") {
    cerrarCierre();
    mostrarLock();
  }
}

// Tres metricas del cierre: esperado, contado y la diferencia (coloreada por resultado).
function mostrarCuadre(tipo, r, mensaje) {
  const etiqueta = { ok: "Cuadra", sobra: "Sobrante", falta: "Faltante" }[tipo];
  cierreResultado.className = `caja-cuadre caja-cuadre--${tipo}`;
  cierreResultado.innerHTML = `
    <div class="cuadre-cards">
      <div class="cuadre-card"><span>Esperado</span><strong>${money(Number(r.esperado))}</strong></div>
      <div class="cuadre-card"><span>Contado</span><strong>${money(Number(r.contado))}</strong></div>
      <div class="cuadre-card cuadre-card--res"><span>${etiqueta}</span><strong>${money(Math.abs(Number(r.diferencia)))}</strong></div>
    </div>
    <p class="cuadre-msg">${mensaje}</p>`;
}

function finalizarTurno() {
  // Cerrado el turno, muestra la pantalla de carga y vuelve al POS,
  // donde aparece de nuevo el modal de apertura de caja (sin pantallas vacias).
  cierreConfirm.disabled = true;
  cajaContado.disabled = true;
  cerrarCierre();
  mostrarCarga();
  setTimeout(() => { window.location.href = "/cajero/pos"; }, 1500);
}

function mostrarCarga() {
  const carga = document.querySelector("#cajaCarga");
  if (carga) carga.classList.remove("hidden");
}

function mostrarLock() {
  cajaLock.classList.remove("hidden");
  setTimeout(() => lockPin.focus(), 50);
}

lockBtn.addEventListener("click", async () => {
  lockError.classList.add("hidden");
  const pin = lockPin.value.trim();
  if (!pin) return;
  lockBtn.disabled = true;
  try {
    await api("POST", "/cajero/caja/desbloquear", { pin });
    cajaLock.classList.add("hidden");
    lockPin.value = "";
    MammaTomatoAlert.success("Caja desbloqueada", "Puedes continuar con tu turno.");
  } catch (err) {
    lockError.textContent = err.message || "Codigo incorrecto";
    lockError.classList.remove("hidden");
  } finally {
    lockBtn.disabled = false;
  }
});

lockPin.addEventListener("keydown", (e) => { if (e.key === "Enter") lockBtn.click(); });

init();
