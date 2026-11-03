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
let loadRevision = 0;
async function load() {
  const revision = ++loadRevision;
  if (!(session.user.operador || session.user.admin)) return;
  $("refresh-panel").disabled = true;
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
    renderAdmin();
  } catch (error) {
    notice(error.message, true);
  } finally {
    if (revision === loadRevision) $("refresh-panel").disabled = false;
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
    ? "Actualizá la información."
    : "Administrá las bajas del catálogo.";
  $("management-description").textContent = session.user.operador
    ? "Agregá y modificá cortes, locales y precios. Las bajas las realiza el administrador."
    : "Podés dar de baja registros. Las altas y modificaciones las realiza el operador.";
  $("admin-new").hidden = !session.user.operador;
  if (!session.user.operador) $("admin-dialog").close();
  if (!session.user.admin) $("delete-dialog").close();
  if (!staff) Object.assign(state, { products: [], stores: [], prices: [] });
  renderAdmin();
  if (staff) load();
}
// The server enforces OPERADOR for create/update and ADMIN for logical deletion.
let editing = null,
  deleting = null;
function adminRows() {
  return state[
    $("admin-kind").value === "productos"
      ? "products"
      : $("admin-kind").value === "locales"
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
  const kind = $("admin-kind").value;
  $("admin-list").innerHTML =
    `<table><thead><tr><th>Registro</th><th>Detalle</th><th>Acciones</th></tr></thead><tbody>${adminRows()
      .map(
        (r) =>
          `<tr><th scope="row">${escape(r.nombre || r.producto.nombre)}</th><td>${kind === "precios" ? `${escape(r.local.nombre)} · ${money(r.precio)} · ${date(r.fecha)}` : escape(r.direccion || "Corte por kilo")}</td><td><div class="admin-actions">${session.user.operador ? `<button class="button secondary small" data-edit="${r.id}">Editar</button>` : ""}${session.user.admin ? `<button class="text-button" data-delete="${r.id}">Dar de baja</button>` : ""}</div></td></tr>`,
      )
      .join(
        "",
      )}</tbody></table>${!adminRows().length ? '<p class="empty">Sin registros activos.</p>' : ""}`;
}
function field(name, label, type, value = "", attrs = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${escape(value)}" ${attrs} required></label>`;
}
function edit(id = null) {
  if (!session.user.operador) return;
  const kind = $("admin-kind").value;
  const record = id === null ? {} : adminRows().find((r) => r.id === id);
  editing = { kind, id };
  $("edit-title").textContent =
    id === null ? "Nuevo registro" : "Editar registro";
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
$("admin-kind").onchange = renderAdmin;
$("admin-new").onclick = () => edit();
$("admin-list").onclick = (event) => {
  const button = event.target.closest("button");
  if (!button) return;
  if (button.dataset.edit) edit(+button.dataset.edit);
  if (button.dataset.delete && session.user.admin) {
    deleting = { kind: $("admin-kind").value, id: +button.dataset.delete };
    $("delete-error").textContent = "";
    $("delete-dialog").showModal();
  }
};
$("admin-form").onsubmit = async (event) => {
  event.preventDefault();
  if (!session.user.operador) return;
  const submit = event.target.querySelector("[type=submit]");
  submit.disabled = true;
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
  }
};
$("confirm-delete").onclick = async () => {
  if (!session.user.admin) return;
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
