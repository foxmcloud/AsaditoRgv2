import {
  $,
  money,
  date,
  escape,
  notice,
  api,
  session,
  openAuth,
  onSessionChange,
  initSession,
} from "./session.js";
const state = { products: [], stores: [], prices: [] };
const sections = {
  precios: {
    title: "Precios",
    singular: "precio",
    action: "Publicar precio",
    description:
      "Cargá el valor por kilo de cada corte y su fecha de referencia.",
    search: "Buscar por corte o comercio…",
    guidance:
      "Cada carga publica un precio nuevo y conserva el historial. No se modifican ni eliminan precios ya publicados.",
  },
  productos: {
    title: "Cortes",
    singular: "corte",
    action: "Agregar corte",
    description:
      "Organizá los productos que la gente encuentra en el comparador.",
    search: "Buscar un corte…",
    guidance:
      "Usá nombres claros y evitá duplicados. Los precios se cargan por separado en el apartado Precios.",
  },
  locales: {
    title: "Locales",
    singular: "local",
    action: "Agregar local",
    description: "Mantené los datos de los comercios y sus ubicaciones.",
    search: "Buscar por nombre o dirección…",
    guidance:
      "Verificá la dirección y las coordenadas antes de guardar. El local quedará disponible para publicar precios.",
  },
};
let activeKind = Object.hasOwn(sections, location.hash.slice(1))
  ? location.hash.slice(1)
  : "precios";
const canCreate = (kind) =>
  kind === "precios" ? session.user.operador : session.user.admin;
const canEdit = (kind) => session.user.admin && kind !== "precios";
const canDelete = canEdit;
let loadRevision = 0;
async function load() {
  const revision = ++loadRevision;
  if (!(session.user.operador || session.user.admin)) return;
  $("refresh-panel").disabled = true;
  $("admin-list").setAttribute("aria-busy", "true");
  try {
    const [products, stores, prices] = await Promise.all(
      ["productos", "locales", "precios"].map((p) => api(`/api/${p}`)),
    );
    if (
      revision !== loadRevision ||
      !(session.user.operador || session.user.admin)
    )
      return;
    Object.assign(state, { products, stores, prices });
    const selected = $("store-filter").value;
    $("store-filter").innerHTML =
      '<option value="">Todos los locales</option>' +
      stores
        .map(
          (store) =>
            `<option value="${store.id}">${escape(store.nombre)}</option>`,
        )
        .join("");
    $("store-filter").value = stores.some(
      (store) => String(store.id) === selected,
    )
      ? selected
      : "";
    renderAdmin();
  } catch (error) {
    notice(error.message, true);
  } finally {
    if (revision === loadRevision) {
      $("refresh-panel").disabled = false;
      $("admin-list").setAttribute("aria-busy", "false");
    }
  }
}
function renderManagementSession() {
  loadRevision++;
  const staff = session.user.admin || session.user.operador;
  $("gestion").hidden = !staff;
  $("panel-access").hidden = Boolean(staff);
  $("access-title").textContent = session.user.authenticated
    ? "Tu cuenta no tiene acceso a la gestión."
    : "Ingresá para gestionar el catálogo.";
  $("access-description").textContent = session.user.authenticated
    ? "Este panel está reservado para operadores y administradores."
    : "Usá tu cuenta de operador o administrador.";
  $("open-panel-login").hidden = Boolean(session.user.authenticated);
  $("management-role").textContent = session.user.operador
    ? "PANEL DE OPERADOR"
    : "PANEL DE ADMINISTRADOR";
  $("admin-title").textContent = session.user.operador
    ? "Cargá los precios del día."
    : "Administrá cortes y locales.";
  $("management-description").textContent = session.user.operador
    ? "Publicá precios para los cortes y locales disponibles. Cada carga conserva el historial."
    : "Agregá, modificá o da de baja cortes y locales. Los precios los carga el operador.";
  if (session.user.operador && !session.user.admin && activeKind !== "precios")
    activeKind = "precios";
  $("admin-new").hidden = !canCreate(activeKind);
  if (
    editing &&
    !(editing.id === null ? canCreate(editing.kind) : canEdit(editing.kind))
  )
    $("admin-dialog").close();
  if (!session.user.admin) $("delete-dialog").close();
  if (!staff) Object.assign(state, { products: [], stores: [], prices: [] });
  renderAdmin();
  if (staff) load();
}
// ADMIN manages cuts/stores; OPERADOR only publishes new prices. The server enforces the same rules.
let editing = null,
  deleting = null;
function adminRows() {
  return state[
    activeKind === "productos"
      ? "products"
      : activeKind === "locales"
        ? "stores"
        : "prices"
  ];
}
function renderAdmin() {
  if (!session.user.admin && !session.user.operador) {
    $("management-summary").replaceChildren();
    $("admin-list").replaceChildren();
    return;
  }
  $("management-summary").innerHTML = [
    ["Cortes activos", state.products.length],
    ["Locales activos", state.stores.length],
    ["Precios publicados", state.prices.length],
  ]
    .map(
      ([label, count]) =>
        `<div><strong>${count}</strong><span>${label}</span></div>`,
    )
    .join("");
  const section = sections[activeKind];
  document.querySelectorAll("[data-section]").forEach((link) => {
    link.hidden = !session.user.admin && link.dataset.section !== "precios";
    if (link.dataset.section === activeKind)
      link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  $("catalog-title").textContent = section.title;
  $("section-step").textContent =
    "CATÁLOGO / " + section.title.toLocaleUpperCase("es");
  $("catalog-description").textContent = section.description;
  $("catalog-guidance").textContent = section.guidance;
  if (session.user.admin && activeKind === "precios")
    $("catalog-guidance").textContent =
      "Consulta del historial de precios. La publicación de precios nuevos está a cargo del operador.";
  $("admin-new").hidden = !canCreate(activeKind);
  $("admin-new").textContent = section.action + " +";
  $("admin-new").disabled =
    activeKind === "precios" &&
    (!state.products.length || !state.stores.length);
  if ($("admin-new").disabled && session.user.operador)
    $("catalog-guidance").textContent =
      "Para publicar precios, el administrador debe cargar al menos un corte y un local.";
  $("catalog-search").placeholder = section.search;
  $("store-filter-label").hidden = activeKind !== "precios";
  const search = $("catalog-search").value.trim().toLocaleLowerCase("es");
  const store = $("store-filter").value;
  const rows = adminRows().filter((r) => {
    const text =
      activeKind === "precios"
        ? `${r.producto.nombre} ${r.local.nombre}`
        : `${r.nombre} ${r.direccion || ""}`;
    return (
      text.toLocaleLowerCase("es").includes(search) &&
      (activeKind !== "precios" || !store || String(r.local.id) === store)
    );
  });
  $("result-count").textContent =
    `${rows.length} de ${adminRows().length} registros`;
  $("clear-filters").hidden = !search && !store;
  const headers =
    activeKind === "precios"
      ? ["Corte", "Local", "Precio / kg", "Fecha"]
      : activeKind === "locales"
        ? ["Local", "Dirección", "Coordenadas"]
        : ["Corte", "Referencia"];
  const hasActions = canEdit(activeKind) || canDelete(activeKind);
  const actions = (r) =>
    `<div class="admin-actions">${canEdit(activeKind) ? `<button class="button secondary small" data-edit="${r.id}" aria-label="Editar ${escape(r.nombre || r.producto.nombre)}">Editar</button>` : ""}${canDelete(activeKind) ? `<button class="text-button" data-delete="${r.id}" aria-label="Dar de baja ${escape(r.nombre || r.producto.nombre)}">Dar de baja</button>` : ""}</div>`;
  const cells = (r) =>
    activeKind === "precios"
      ? `<th scope="row">${escape(r.producto.nombre)}</th><td>${escape(r.local.nombre)}</td><td class="price-cell">${money(r.precio)}</td><td>${date(r.fecha)}</td>`
      : activeKind === "locales"
        ? `<th scope="row">${escape(r.nombre)}</th><td>${escape(r.direccion)}</td><td class="muted">${escape(r.coordenada_x)}, ${escape(r.coordenada_y)}</td>`
        : `<th scope="row">${escape(r.nombre)}</th><td class="muted">Corte #${r.id} · Precio por kilo</td>`;
  $("admin-list").innerHTML = rows.length
    ? `<table><caption class="sr-only">Registros de ${section.title}</caption><thead><tr>${headers.map((h) => `<th scope="col">${h}</th>`).join("")}${hasActions ? '<th scope="col">Acciones</th>' : ""}</tr></thead><tbody>${rows.map((r) => `<tr>${cells(r)}${hasActions ? `<td>${actions(r)}</td>` : ""}</tr>`).join("")}</tbody></table>`
    : `<div class="empty"><strong>${adminRows().length ? "No encontramos coincidencias." : "Todavía no hay registros en este apartado."}</strong>${adminRows().length ? "Probá con otra búsqueda o limpiá los filtros." : session.user.operador ? "Usá el botón de arriba para cargar la primera información." : "Cuando el operador cargue información, aparecerá acá."}</div>`;
}
function changeSection() {
  const kind = location.hash.slice(1);
  activeKind =
    Object.hasOwn(sections, kind) && (session.user.admin || kind === "precios")
      ? kind
      : "precios";
  $("catalog-search").value = "";
  $("store-filter").value = "";
  renderAdmin();
}
window.addEventListener("hashchange", changeSection);
$("catalog-search").oninput = renderAdmin;
$("store-filter").onchange = renderAdmin;
$("clear-filters").onclick = () => {
  $("catalog-search").value = "";
  $("store-filter").value = "";
  renderAdmin();
  $("catalog-search").focus();
};
function field(name, label, type, value = "", attrs = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${escape(value)}" ${attrs} required></label>`;
}
function edit(id = null) {
  if (!(id === null ? canCreate(activeKind) : canEdit(activeKind))) return;
  const kind = activeKind;
  const record = id === null ? {} : adminRows().find((r) => r.id === id);
  editing = { kind, id };
  $("edit-title").textContent =
    (id === null ? "Nuevo " : "Editar ") + sections[kind].singular;
  $("edit-help").textContent = sections[kind].guidance;
  $("admin-error").textContent = "";
  let html;
  if (kind === "precios") {
    const select = (name, label, values, selected) =>
      `<label>${label}<select name="${name}" required><option value="">Seleccioná…</option>${values.map((v) => `<option value="${v.id}" ${selected === v.id ? "selected" : ""}>${escape(v.nombre)}</option>`).join("")}</select></label>`;
    html =
      select("localId", "Local", state.stores, record.local?.id) +
      select("productoId", "Corte", state.products, record.producto?.id) +
      field(
        "precio",
        "Precio por kilo (ARS)",
        "number",
        record.precio,
        'min="0.01" max="9999999999.99" step="0.01"',
      ) +
      field(
        "fecha",
        "Fecha del precio",
        "date",
        record.fecha ||
          new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
            .toISOString()
            .slice(0, 10),
        'max="' +
          new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
            .toISOString()
            .slice(0, 10) +
          '"',
      );
  } else {
    html = field("nombre", "Nombre", "text", record.nombre, 'maxlength="120"');
    if (kind === "locales")
      html +=
        field(
          "direccion",
          "Dirección",
          "text",
          record.direccion,
          'maxlength="200"',
        ) +
        field(
          "latitud",
          "Latitud",
          "number",
          record.coordenada_x,
          'min="-90" max="90" step="any"',
        ) +
        field(
          "longitud",
          "Longitud",
          "number",
          record.coordenada_y,
          'min="-180" max="180" step="any"',
        );
  }
  $("admin-fields").innerHTML = html;
  $("admin-dialog").showModal();
}
$("admin-new").onclick = () => edit();
$("admin-list").onclick = (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  if (button.dataset.edit) edit(+button.dataset.edit);
  if (button.dataset.delete && canDelete(activeKind)) {
    deleting = { kind: activeKind, id: +button.dataset.delete };
    const record = adminRows().find((r) => r.id === deleting.id);
    $("delete-record").textContent =
      record.nombre ||
      `${record.producto.nombre} · ${record.local.nombre} · ${money(record.precio)}`;
    $("delete-error").textContent = "";
    $("delete-dialog").showModal();
  }
};
$("admin-form").onsubmit = async (event) => {
  event.preventDefault();
  if (
    !editing ||
    !(editing.id === null ? canCreate(editing.kind) : canEdit(editing.kind))
  )
    return;
  const submit = event.target.querySelector("[type=submit]");
  submit.disabled = true;
  submit.textContent = "Guardando…";
  const data = Object.fromEntries(new FormData(event.target));
  ["localId", "productoId", "precio", "latitud", "longitud"].forEach((key) => {
    if (key in data) data[key] = Number(data[key]);
  });
  try {
    await api(
      `/api/admin/${editing.kind}${editing.id ? `/${editing.id}` : ""}`,
      { method: editing.id ? "PUT" : "POST", body: JSON.stringify(data) },
    );
    $("admin-dialog").close();
    await load();
    notice(
      "Registro guardado. La información ya está disponible en el comparador.",
    );
  } catch (error) {
    $("admin-error").textContent = error.message;
  } finally {
    submit.disabled = false;
    submit.textContent = "Guardar registro";
  }
};
$("confirm-delete").onclick = async () => {
  if (!deleting || !canDelete(deleting.kind)) return;
  $("confirm-delete").disabled = true;
  try {
    await api(`/api/admin/${deleting.kind}/${deleting.id}`, {
      method: "DELETE",
    });
    $("delete-dialog").close();
    await load();
    notice("Baja lógica aplicada. El registro se conserva en la base.");
  } catch (error) {
    $("delete-error").textContent = error.message;
  } finally {
    $("confirm-delete").disabled = false;
  }
};
$("refresh-panel").onclick = load;
$("open-panel-login").onclick = openAuth;
$("auth-toggle").hidden = true;
onSessionChange(renderManagementSession);
initSession();
