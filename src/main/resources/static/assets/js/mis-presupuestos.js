import { $, money, date, escape, api, session, notice, openAuth, onSessionChange, initSession } from "./session.js";
import { listadoGuardado, adaptarPresupuestoGuardado } from "./presupuesto-guardado.js";

const PRESUPUESTOS_URL = "/api/presupuestos";
const state = { filas: [], pagina: 0, paginas: 1, usuario: null, revision: 0, consulta: 0, detalle: 0, ocupados: new Set(), iniciada: false };
const cantidad = n => `${new Intl.NumberFormat("es-AR", { maximumFractionDigits: 2 }).format(n)} kg`;
function informar(texto, error = false) {
  // Los descendientes del diálogo se muestran sobre su backdrop nativo.
  const destino = $("detalle-dialogo").open ? $("detalle-dialogo") : document.body;
  destino.append($("notice"));
  notice(texto, error);
}
function mensaje(titulo, texto, accion = "") {
  $("listado").innerHTML = `<div class="history-empty"><span aria-hidden="true">↺</span><h3>${escape(titulo)}</h3><p>${escape(texto)}</p>${accion}</div>`;
}
function actualizarBotonesPdf() {
  document.querySelectorAll("[data-pdf]").forEach(boton => {
    const ocupado = state.ocupados.has(Number(boton.dataset.pdf));
    boton.disabled = ocupado;
    boton.textContent = ocupado ? "Generando PDF…" : "Regenerar PDF";
  });
}
function renderizar() {
  const filtro = $("buscar").value.trim().toLocaleLowerCase("es-AR");
  const orden = $("orden").value;
  const filas = state.filas.filter(p => `${p.id} ${p.localNombre}`.toLocaleLowerCase("es-AR").includes(filtro));
  filas.sort((a, b) => {
    if (orden === "menor") return a.total - b.total || a.id - b.id;
    if (orden === "mayor") return b.total - a.total || b.id - a.id;
    const diferencia = (a.fecha || "").localeCompare(b.fecha || "") || a.id - b.id;
    return orden === "antiguo" ? diferencia : -diferencia;
  });
  $("conteo").textContent = `${filas.length} de ${state.filas.length} presupuestos en esta página`;
  if (!state.filas.length) {
    mensaje("La próxima juntada empieza acá", "Todavía no tenés presupuestos guardados. Armá uno desde la calculadora.", '<a href="/#calculadora" class="button">Armar un presupuesto ↗</a>');
    return;
  }
  if (!filas.length) {
    mensaje("No encontramos coincidencias", "Probá con otro comercio o número de presupuesto.", '<button class="button secondary" data-action="limpiar">Limpiar búsqueda</button>');
    return;
  }
  $("listado").innerHTML = filas.map(p => `<article class="saved-budget">
    <div class="saved-budget-top"><span class="saved-budget-number">N.º ${p.id}</span><span class="saved-budget-date">${escape(date(p.fecha))}</span></div>
    <h3>${escape(p.localNombre)}</h3><p class="saved-budget-address">${escape(p.localDireccion || "Dirección no informada")}</p>
    <div class="saved-budget-total"><span>Total guardado · ARS</span><strong>${money(p.total)}</strong></div>
    <div class="saved-budget-actions"><button class="button secondary small" data-ver="${p.id}" aria-label="Ver detalle del presupuesto ${p.id}">Ver detalle</button><button class="button small" data-pdf="${p.id}" aria-label="Regenerar PDF del presupuesto ${p.id}">Regenerar PDF</button></div>
  </article>`).join("");
  actualizarBotonesPdf();
}
async function cargar(pagina = 0) {
  if (!state.usuario) return;
  const consulta = ++state.consulta, revision = state.revision;
  $("actualizar").disabled = $("buscar").disabled = $("orden").disabled = true;
  $("anterior").disabled = $("siguiente").disabled = true;
  $("listado").setAttribute("aria-busy", "true");
  $("conteo").textContent = "Buscando tus presupuestos…";
  $("listado").innerHTML = Array.from({ length: 3 }, () => '<div class="history-skeleton" aria-hidden="true"></div>').join("");
  try {
    const datos = listadoGuardado(await api(`${PRESUPUESTOS_URL}?page=${pagina}&size=12`, { cache: "no-store" }), pagina);
    if (revision !== state.revision || consulta !== state.consulta) return;
    Object.assign(state, datos);
    renderizar();
    $("paginacion").hidden = state.paginas <= 1;
    $("pagina").textContent = `Página ${state.pagina + 1} de ${state.paginas}`;
    $("anterior").disabled = state.pagina === 0;
    $("siguiente").disabled = state.pagina + 1 >= state.paginas;
    $("buscar").disabled = $("orden").disabled = false;
  } catch (error) {
    if (revision !== state.revision || consulta !== state.consulta) return;
    state.filas = [];
    $("conteo").textContent = "El historial no está disponible";
    $("paginacion").hidden = true;
    mensaje("No pudimos consultar tus presupuestos", "Podés volver a intentarlo en unos instantes.", '<button class="button secondary" data-action="reintentar">Reintentar</button>');
    informar(error.message, true);
  } finally {
    if (revision === state.revision && consulta === state.consulta) {
      $("actualizar").disabled = false;
      $("listado").setAttribute("aria-busy", "false");
    }
  }
}
async function consultarDetalle(id) {
  const p = adaptarPresupuestoGuardado(await api(`${PRESUPUESTOS_URL}/${encodeURIComponent(id)}`, { cache: "no-store" }));
  if (p.presupuestoId !== id) throw new Error("La respuesta no corresponde al presupuesto solicitado.");
  return p;
}
async function abrirDetalle(id) {
  const consulta = ++state.detalle, revision = state.revision;
  const dialogo = $("detalle-dialogo");
  $("detalle-titulo").textContent = `Presupuesto N.º ${id}`;
  $("detalle-contenido").textContent = "Cargando tus cortes y cantidades…";
  $("detalle-pdf").disabled = true;
  delete $("detalle-pdf").dataset.pdf;
  if (!dialogo.open) dialogo.showModal();
  try {
    const p = await consultarDetalle(id);
    if (revision !== state.revision || consulta !== state.detalle || !dialogo.open) return;
    $("detalle-titulo").textContent = p.localNombre;
    $("detalle-contenido").innerHTML = `<p class="muted">Presupuesto N.º ${p.presupuestoId} · Guardado el ${date(p.generado)}</p><p class="saved-detail-address">${escape(p.direccion)}</p>
      <ul class="saved-detail-items">${p.items.map(item => `<li><h3>${escape(item.nombre)}</h3><dl><div><dt>Cantidad</dt><dd>${cantidad(item.cantidad)}</dd></div><div><dt>Precio por kilo</dt><dd>${money(item.precio)}</dd></div><div><dt>Subtotal</dt><dd>${money(item.subtotal)}</dd></div></dl></li>`).join("")}</ul>
      <div class="saved-detail-total"><span>Total guardado</span><strong>${money(p.total)}</strong></div><p class="muted">Estos importes pertenecen al presupuesto original. Confirmá vigencia y disponibilidad antes de comprar.</p>`;
    $("detalle-pdf").dataset.pdf = String(id);
    actualizarBotonesPdf();
  } catch (error) {
    if (revision !== state.revision || consulta !== state.detalle || !dialogo.open) return;
    $("detalle-contenido").textContent = error.status === 404 ? "El presupuesto no existe o no pertenece a tu cuenta." : "No pudimos recuperar el detalle. Cerrá esta ventana y volvé a intentarlo.";
    informar(error.message, true);
  }
}
async function descargar(id) {
  if (!state.usuario || state.ocupados.has(id)) return;
  const revision = state.revision;
  state.ocupados.add(id);
  actualizarBotonesPdf();
  try {
    // Cada descarga vuelve a pasar por el control de propiedad de tu GET.
    const p = await consultarDetalle(id);
    if (revision !== state.revision) return;
    const { generarPDF } = await import("./reporte.js");
    const blob = await generarPDF(p);
    if (revision !== state.revision) return;
    const url = URL.createObjectURL(blob), enlace = document.createElement("a");
    enlace.href = url;
    enlace.download = `asadito-presupuesto-${id}.pdf`;
    document.body.append(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 60000);
    informar("PDF regenerado con los importes guardados. Ya podés adjuntarlo en WhatsApp.");
  } catch (error) {
    if (revision === state.revision) informar(error.status === 404 ? "El presupuesto no existe o no pertenece a tu cuenta." : error.message, true);
  } finally {
    if (revision === state.revision) {
      state.ocupados.delete(id);
      actualizarBotonesPdf();
    }
  }
}
function sesionActualizada() {
  state.iniciada = true;
  state.revision++;
  state.consulta++;
  state.detalle++;
  state.usuario = session.user.authenticated ? session.user.email : null;
  state.ocupados.clear();
  state.filas = [];
  if ($("detalle-dialogo").open) $("detalle-dialogo").close();
  $("paginacion").hidden = true;
  $("listado").setAttribute("aria-busy", "false");
  $("buscar").value = "";
  $("buscar").disabled = $("orden").disabled = $("actualizar").disabled = !state.usuario;
  if (state.usuario) {
    cargar();
  } else {
    $("conteo").textContent = "Tu historial es privado";
    mensaje("Tus presupuestos te esperan", "Ingresá con tu cuenta para consultar tus planes y regenerar sus PDF.", '<button class="button" data-action="ingresar">Ingresar a mi cuenta</button>');
  }
}
$("actualizar").onclick = () => cargar(state.pagina);
$("anterior").onclick = () => cargar(state.pagina - 1);
$("siguiente").onclick = () => cargar(state.pagina + 1);
$("buscar").oninput = renderizar;
$("orden").onchange = renderizar;
$("detalle-dialogo").addEventListener("close", () => {
  state.detalle++;
  document.body.append($("notice"));
});
$("detalle-dialogo").addEventListener("click", event => {
  if (event.target === $("detalle-dialogo")) $("detalle-dialogo").close();
});
document.addEventListener("click", event => {
  const boton = event.target.closest("button");
  if (!boton || boton.disabled) return;
  if (boton.dataset.ver) abrirDetalle(Number(boton.dataset.ver));
  if (boton.dataset.pdf) descargar(Number(boton.dataset.pdf));
  if (boton.dataset.action === "ingresar") openAuth();
  if (boton.dataset.action === "reintentar") cargar(state.pagina);
  if (boton.dataset.action === "limpiar") { $("buscar").value = ""; renderizar(); $("buscar").focus(); }
});
onSessionChange(sesionActualizada);
initSession().then(() => {
  if (!state.iniciada) {
    $("conteo").textContent = "No pudimos comprobar tu sesión";
    $("listado").setAttribute("aria-busy", "false");
    mensaje("No pudimos conectar", "Recargá la página para volver a intentar la conexión.");
  }
});
