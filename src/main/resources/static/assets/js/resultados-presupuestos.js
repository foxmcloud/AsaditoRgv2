import { $, api, session, date, money, escape, openAuth, onSessionChange, initSession } from "./session.js";
import { listadoGuardado } from "./presupuesto-guardado.js";
import { consultarResultados, resultadoHTML, prepararVerificacion, notificarVerificacion } from "./verificaciones-ui.js";
import { resumirResultados } from "./verificaciones-modelo.js";
const state = { filas: [], pagina: 0, paginas: 1, usuario: null, revision: 0, consulta: 0, iniciada: false };
const formulario = prepararVerificacion({
  vigente: usuario => state.usuario === usuario && session.user.authenticated,
  alGuardar: (id, verificacion) => {
    const fila = state.filas.find(f => f.presupuesto.id === id);
    if (fila) { Object.assign(fila, { verificacion, estado: "verificado" }); renderizar(); }
  },
});
function mensaje(titulo, texto, accion = "") {
  $("resultados-listado").innerHTML = `<div class="history-empty"><h3>${escape(titulo)}</h3><p>${escape(texto)}</p>${accion}</div>`;
}
function comparacion(v) {
  const maximo = Math.max(v.totalEstimado, v.totalReal);
  return `<div class="verification-bars" aria-label="Comparación entre estimado y pagado">
    <div><span>Estimado</span><strong>${money(v.totalEstimado)}</strong><i style="width:${v.totalEstimado / maximo * 100}%"></i></div>
    <div><span>Pagado</span><strong>${money(v.totalReal)}</strong><i style="width:${v.totalReal / maximo * 100}%"></i></div>
  </div>`;
}
function renderizar() {
  const resumen = resumirResultados(state.filas);
  $("resumen-verificados").textContent = resumen.verificadas;
  $("resumen-pendientes").textContent = resumen.pendientes;
  $("resumen-acordes").textContent = resumen.acordes;
  $("resumen-importes").textContent = resumen.verificadas ? `En los presupuestos verificados de esta página: estimaste ${money(resumen.estimado)} y pagaste ${money(resumen.pagado)}.` : "Los totales comparados aparecerán cuando registres tu primera compra.";
  const filtro = $("buscar-resultados").value.trim().toLocaleLowerCase("es-AR"), estado = $("filtro-resultado").value;
  const filas = state.filas.filter(fila => {
    const p = fila.presupuesto;
    return `${p.id} ${p.localNombre}`.toLocaleLowerCase("es-AR").includes(filtro)
      && (estado === "todos" || fila.estado === estado || fila.verificacion?.valoracion === estado);
  });
  filas.sort((a, b) => (b.presupuesto.fecha || "").localeCompare(a.presupuesto.fecha || "") || b.presupuesto.id - a.presupuesto.id);
  $("resultados-conteo").textContent = `${filas.length} de ${state.filas.length} presupuestos en esta página${resumen.errores ? ` · ${resumen.errores} resultados no disponibles` : ""}`;
  if (!state.filas.length) {
    mensaje("Todavía no hay compras para comparar", "Primero guardá un presupuesto desde la calculadora.", '<a class="button" href="/#calculadora">Armar un presupuesto</a>');
    return;
  }
  if (!filas.length) {
    mensaje("No hay resultados para esos filtros", "Probá otro comercio o estado.", '<button class="button secondary" data-limpiar>Limpiar filtros</button>');
    return;
  }
  $("resultados-listado").innerHTML = filas.map(fila => {
    const p = fila.presupuesto, v = fila.verificacion;
    return `<article class="saved-budget result-card">
      <div class="saved-budget-top"><span class="saved-budget-number">N.º ${p.id}</span><span class="saved-budget-date">${date(p.fecha)}</span></div>
      <h3>${escape(p.localNombre)}</h3><p class="saved-budget-address">${escape(p.localDireccion)}</p>
      ${v ? comparacion(v) + resultadoHTML(v, true) : `<div class="verification-pending"><span class="verification-badge pendiente">${fila.estado === "error" ? "Resultado no disponible" : "Sin verificar"}</span><p>${fila.estado === "error" ? "No pudimos consultar la verificación. Volvé a intentarlo." : "Cuando hagas la compra, registrá cuánto pagaste."}</p><strong>Estimado: ${money(p.total)}</strong></div>`}
      <div class="saved-budget-actions"><a class="button secondary small" href="/mis-presupuestos.html?presupuesto=${p.id}">Ver presupuesto</a><button class="button small" data-verificar="${p.id}">${v ? "Ver resultado" : fila.estado === "error" ? "Volver a consultar" : "Verificar compra"}</button></div>
    </article>`;
  }).join("");
}
function controles(disabled) {
  for (const id of ["buscar-resultados", "filtro-resultado", "resultados-actualizar"]) $(id).disabled = disabled;
}
async function cargar(pagina = 0) {
  if (!state.usuario) return;
  const revision = state.revision, consulta = ++state.consulta;
  const vigente = () => revision === state.revision && consulta === state.consulta;
  controles(true);
  $("anterior").disabled = $("siguiente").disabled = true;
  $("resultados-listado").setAttribute("aria-busy", "true");
  $("resultados-conteo").textContent = "Consultando tus presupuestos y verificaciones…";
  $("resultados-listado").innerHTML = '<div class="history-skeleton" aria-hidden="true"></div><div class="history-skeleton" aria-hidden="true"></div>';
  try {
    const listado = listadoGuardado(await api(`/api/presupuestos?page=${pagina}&size=12`, { cache: "no-store" }), pagina);
    if (!vigente()) return;
    const filas = await consultarResultados(listado.filas, vigente);
    if (!vigente()) return;
    Object.assign(state, { filas, pagina: listado.pagina, paginas: listado.paginas });
    renderizar();
    $("paginacion").hidden = state.paginas <= 1;
    $("pagina").textContent = `Página ${state.pagina + 1} de ${state.paginas}`;
    $("anterior").disabled = state.pagina === 0;
    $("siguiente").disabled = state.pagina + 1 >= state.paginas;
  } catch (error) {
    if (!vigente()) return;
    state.filas = [];
    $("paginacion").hidden = true;
    $("resultados-conteo").textContent = "No pudimos cargar los resultados";
    mensaje("No pudimos conectar", "Tus presupuestos se conservan. Volvé a intentarlo.", '<button class="button secondary" data-reintentar>Reintentar</button>');
    notificarVerificacion(error.message, true);
  } finally {
    if (vigente()) { controles(false); $("resultados-listado").setAttribute("aria-busy", "false"); }
  }
}
function sesionActualizada() {
  state.iniciada = true;
  state.revision++; state.consulta++;
  state.usuario = session.user.authenticated ? session.user.email : null;
  state.filas = [];
  formulario.cerrar();
  $("paginacion").hidden = true;
  $("resultados-listado").setAttribute("aria-busy", "false");
  $("buscar-resultados").value = "";
  $("filtro-resultado").value = "todos";
  for (const id of ["resumen-verificados", "resumen-pendientes", "resumen-acordes"]) $(id).textContent = "—";
  $("resumen-importes").textContent = "";
  controles(!state.usuario);
  if (state.usuario) cargar();
  else {
    $("resultados-conteo").textContent = "Tus resultados son privados";
    mensaje("Ingresá para ver tus resultados", "Solo tu cuenta puede consultar y verificar tus presupuestos.", '<button class="button" data-ingresar>Ingresar a mi cuenta</button>');
  }
}
$("resultados-actualizar").onclick = () => cargar(state.pagina);
$("buscar-resultados").oninput = renderizar;
$("filtro-resultado").onchange = renderizar;
$("anterior").onclick = () => cargar(state.pagina - 1);
$("siguiente").onclick = () => cargar(state.pagina + 1);
document.addEventListener("click", event => {
  const boton = event.target.closest("button");
  if (!boton || boton.disabled) return;
  if (boton.dataset.verificar) {
    const fila = state.filas.find(f => f.presupuesto.id === Number(boton.dataset.verificar));
    if (fila) formulario.abrir(fila.presupuesto);
  }
  if (boton.hasAttribute("data-reintentar")) cargar(state.pagina);
  if (boton.hasAttribute("data-ingresar")) openAuth();
  if (boton.hasAttribute("data-limpiar")) { $("buscar-resultados").value = ""; $("filtro-resultado").value = "todos"; renderizar(); }
});
onSessionChange(sesionActualizada);
initSession().then(() => {
  if (!state.iniciada) {
    $("resultados-conteo").textContent = "No pudimos comprobar tu sesión";
    $("resultados-listado").setAttribute("aria-busy", "false");
    mensaje("No pudimos conectar", "Recargá la página para volver a intentarlo.");
  }
});
