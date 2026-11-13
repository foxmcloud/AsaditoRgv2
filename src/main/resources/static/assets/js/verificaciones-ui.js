import { $, api, money, date, escape, notice, session } from "./session.js";
import { normalizarVerificacion, valoraciones } from "./verificaciones-modelo.js";

export async function obtenerVerificacion(id) {
  try {
    return normalizarVerificacion(await api(`/api/presupuestos/${id}/verificacion`, { cache: "no-store" }), id);
  } catch (error) {
    if (error.status === 404) return null;
    throw error;
  }
}
export async function consultarResultados(presupuestos, vigente = () => true) {
  const filas = new Array(presupuestos.length);
  let siguiente = 0;
  async function consultar() {
    while (siguiente < presupuestos.length && vigente()) {
      const indice = siguiente++, presupuesto = presupuestos[indice];
      try {
        const verificacion = await obtenerVerificacion(presupuesto.id);
        filas[indice] = { presupuesto, verificacion, estado: verificacion ? "verificado" : "pendiente" };
      } catch (error) {
        filas[indice] = { presupuesto, estado: "error", error };
      }
    }
  }
  await Promise.all(Array.from({ length: Math.min(4, presupuestos.length) }, consultar));
  return filas;
}
export const porcentaje = valor => `${valor > 0 ? "+" : ""}${new Intl.NumberFormat("es-AR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(valor)} %`;
export const diferencia = valor => `${valor > 0 ? "+" : ""}${money(valor)}`;
export function resultadoHTML(v, compacto = false) {
  const info = valoraciones[v.valoracion];
  return `<section class="verification-summary ${info.clase}" aria-label="Resultado de la verificación">
    <span class="verification-badge ${info.clase}">${escape(info.titulo)}</span>
    <p>${escape(info.detalle)}</p>
    <dl class="verification-numbers"><div><dt>Estimado</dt><dd>${money(v.totalEstimado)}</dd></div><div><dt>Pagado</dt><dd>${money(v.totalReal)}</dd></div><div><dt>Diferencia</dt><dd>${diferencia(v.diferencia)}</dd></div></dl>
    <div class="verification-caption"><strong>${porcentaje(v.porcentaje)}</strong><span>Verificado el ${date(v.fecha)}</span></div>
    ${!compacto && v.observacion ? `<div class="verification-observation"><strong>Tu observación</strong><p>${escape(v.observacion)}</p></div>` : ""}
  </section>`;
}
export function notificarVerificacion(texto, error = false) {
  const abiertos = [...document.querySelectorAll("dialog[open]")];
  (abiertos.at(-1) || document.body).append($("notice"));
  notice(texto, error);
}
export function prepararVerificacion({ alGuardar, vigente }) {
  const dialogo = $("verificacion-dialogo"), form = $("verificacion-form");
  let actual = null, consulta = 0, guardando = false;
  function deshabilitar(valor) {
    [...form.elements].forEach(elemento => { elemento.disabled = valor; });
  }
  function mostrar(v) {
    form.hidden = true;
    $("verificacion-cargando").hidden = true;
    $("verificacion-resultado").innerHTML = resultadoHTML(v);
    $("verificacion-reintentar").hidden = true;
    $("verificacion-error").textContent = "";
  }
  async function abrir(presupuesto) {
    const revision = ++consulta, usuario = session.user.email;
    actual = presupuesto;
    guardando = false;
    form.reset();
    deshabilitar(false);
    form.hidden = true;
    $("verificacion-titulo").textContent = `Presupuesto N.º ${presupuesto.id}`;
    $("verificacion-local").textContent = presupuesto.localNombre;
    $("verificacion-estimado").textContent = money(presupuesto.total);
    $("verificacion-resultado").replaceChildren();
    $("verificacion-error").textContent = "";
    $("verificacion-cargando").hidden = false;
    $("verificacion-reintentar").hidden = true;
    if (!dialogo.open) dialogo.showModal();
    try {
      const v = await obtenerVerificacion(presupuesto.id);
      if (revision !== consulta || !dialogo.open || !vigente(usuario)) return;
      $("verificacion-cargando").hidden = true;
      if (v) mostrar(v);
      else { form.hidden = false; $("verificacion-total").focus(); }
    } catch (error) {
      if (revision !== consulta || !dialogo.open || !vigente(usuario)) return;
      $("verificacion-cargando").hidden = true;
      $("verificacion-error").textContent = error.message;
      $("verificacion-reintentar").hidden = false;
    }
  }
  form.addEventListener("submit", async event => {
    event.preventDefault();
    if (!actual || guardando || !form.reportValidity()) return;
    const presupuesto = actual, revision = consulta, usuario = session.user.email;
    const datos = new URLSearchParams(new FormData(form));
    guardando = true;
    deshabilitar(true);
    $("verificacion-submit").textContent = "Guardando…";
    $("verificacion-error").textContent = "";
    try {
      const v = normalizarVerificacion(await api(`/api/presupuestos/${presupuesto.id}/verificacion`, { method: "POST", body: datos }), presupuesto.id);
      if (!vigente(usuario)) return;
      alGuardar(presupuesto.id, v);
      if (revision === consulta && dialogo.open) mostrar(v);
      notificarVerificacion("Verificación guardada. Ya podés consultar el resultado en Mis resultados.");
    } catch (error) {
      if (revision !== consulta || !dialogo.open || !vigente(usuario)) return;
      $("verificacion-error").textContent = error.message;
      $("verificacion-reintentar").hidden = false;
      if (error.status === 409) form.hidden = true;
    } finally {
      if (revision === consulta) {
        guardando = false;
        deshabilitar(false);
        $("verificacion-submit").textContent = "Guardar verificación";
      }
    }
  });
  $("verificacion-reintentar").onclick = () => { if (actual) abrir(actual); };
  dialogo.addEventListener("close", () => {
    consulta++;
    actual = null;
    guardando = false;
    form.reset();
    const abiertos = [...document.querySelectorAll("dialog[open]")];
    (abiertos.at(-1) || document.body).append($("notice"));
  });
  dialogo.addEventListener("click", event => { if (event.target === dialogo) dialogo.close(); });
  return { abrir, cerrar: () => { if (dialogo.open) dialogo.close(); } };
}
