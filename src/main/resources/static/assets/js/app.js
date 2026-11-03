const $ = (id) => document.getElementById(id);
const money = (value) =>
  new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(
    value,
  );
const date = (value) =>
  value
    ? new Intl.DateTimeFormat("es-AR").format(new Date(`${value}T12:00:00`))
    : "Sin fecha";
const state = {
  products: [],
  stores: [],
  prices: [],
  latest: [],
  quantities: {},
  quotes: [],
  selected: null,
  user: {},
  csrf: null,
  pdf: null,
  register: false,
  revision: 0,
  busy: false,
};
const escape = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
function notice(message, error = false) {
  $("notice").textContent = message;
  $("notice").className = error ? "error" : "";
  $("notice").hidden = false;
}
async function refreshCsrf() {
  const response = await fetch("/api/auth/csrf", {
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok)
    throw new Error("No se pudo iniciar la sesión. Recargá la página.");
  state.csrf = await response.json();
}
async function api(path, options = {}) {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof URLSearchParams))
    headers.set("Content-Type", "application/json");
  if (options.method && options.method !== "GET") {
    if (!state.csrf) await refreshCsrf();
    headers.set(state.csrf.headerName, state.csrf.token);
  }
  const response = await fetch(path, {
    credentials: "same-origin",
    ...options,
    headers,
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const fields = Object.entries(body.fields || {})
      .map(([field, error]) => `${field}: ${error}`)
      .join(" · ");
    if (response.status === 401) {
      state.user = {};
      renderSession();
    }
    if (response.status === 403) {
      state.csrf = null;
    }
    throw new Error(
      [
        body.message ||
          `No se pudo completar la operación (${response.status})`,
        fields,
      ]
        .filter(Boolean)
        .join(". "),
    );
  }
  if (response.status === 204) return null;
  return options.pdf ? response.blob() : response.json();
}
function invalidate() {
  state.revision++;
  state.quotes = [];
  state.selected = null;
  state.pdf = null;
  renderQuote();
  $("store-results").replaceChildren();
}
async function load() {
  $("refresh").disabled = true;
  try {
    const [products, stores, prices, latest] = await Promise.all(
      ["productos", "locales", "precios", "precios/ultimos"].map((p) =>
        api(`/api/${p}`),
      ),
    );
    Object.assign(state, { products, stores, prices, latest });
    // Never retain quantities for products that have been deactivated.
    state.quantities = Object.fromEntries(
      Object.entries(state.quantities).filter(([id]) =>
        products.some((p) => p.id === +id),
      ),
    );
    invalidate();
    renderComparison();
    renderBasket();
    renderAdmin();
  } catch (error) {
    notice(error.message, true);
    $("comparison").innerHTML =
      '<p class="empty">No pudimos cargar el catálogo. Intentá actualizar.</p>';
  } finally {
    $("refresh").disabled = false;
  }
}
function renderComparison() {
  const search = $("search").value.trim().toLocaleLowerCase("es");
  const products = state.products.filter((p) =>
    p.nombre.toLocaleLowerCase("es").includes(search),
  );
  const stores = state.latest;
  $("data-count").textContent =
    `${state.stores.length} locales · ${state.products.length} cortes`;
  if (!stores.length) {
    $("comparison").innerHTML =
      '<div class="empty"><strong>Todavía no hay precios publicados.</strong>Los locales y cortes ya están disponibles. Un administrador debe cargar los precios para comenzar a comparar.</div>';
    return;
  }
  if (!products.length) {
    $("comparison").innerHTML =
      '<p class="empty">No encontramos cortes con ese nombre.</p>';
    return;
  }
  $("comparison").innerHTML =
    `<table><caption class="fine-print">Precios por kilo en ARS · Deslizá para ver todos los locales</caption><thead><tr><th scope="col">Corte / kg</th>${stores.map((s) => `<th scope="col">${escape(s.localNombre)}</th>`).join("")}</tr></thead><tbody>${products
      .map((p) => {
        const values = stores.flatMap((s) =>
          s.productos.filter((i) => i.id === p.id).map((i) => Number(i.precio)),
        );
        const minimum = values.length ? Math.min(...values) : null;
        return `<tr><th scope="row">${escape(p.nombre)}</th>${stores
          .map((s) => {
            const price = s.productos.find((i) => i.id === p.id);
            return price
              ? `<td class="${Number(price.precio) === minimum ? "best" : ""}">${money(price.precio)}${Number(price.precio) === minimum ? '<span class="best-tag">MENOR</span>' : ""}<small>${date(price.fecha)}</small></td>`
              : '<td class="muted">Sin precio</td>';
          })
          .join("")}</tr>`;
      })
      .join("")}</tbody></table>`;
}
function renderBasket() {
  $("basket").innerHTML = state.products.length
    ? state.products
        .map(
          (p) =>
            `<div class="basket-row"><label for="kg-${p.id}">${escape(p.nombre)}</label><div class="quantity"><input type="number" id="kg-${p.id}" data-product="${p.id}" min="0" max="1000" step="0.001" inputmode="decimal" value="${state.quantities[p.id] || 0}" aria-label="Kilos de ${escape(p.nombre)}"><span>kg</span></div></div>`,
        )
        .join("")
    : '<p class="empty">No hay cortes disponibles.</p>';
}
function payload() {
  return {
    localId: state.selected?.localId || 0,
    items: Object.entries(state.quantities)
      .filter(([, q]) => q > 0)
      .map(([id, q]) => ({ productoId: +id, cantidad: q })),
  };
}
async function calculate() {
  if (state.busy) return;
  if (
    Object.values(state.quantities).some(
      (q) =>
        !Number.isFinite(q) ||
        q < 0 ||
        q > 1000 ||
        Math.abs(q * 1000 - Math.round(q * 1000)) > 0.000001,
    )
  ) {
    notice(
      "Revisá los kilos: usá valores de 0 a 1000 con hasta tres decimales.",
      true,
    );
    return;
  }
  const request = payload();
  if (!request.items.length) {
    notice("Elegí al menos un corte e ingresá los kilos.", true);
    return;
  }
  if (
    request.items.some(
      (i) =>
        !Number.isFinite(i.cantidad) ||
        i.cantidad > 1000 ||
        i.cantidad < 0.001 ||
        Math.abs(i.cantidad * 1000 - Math.round(i.cantidad * 1000)) > 0.000001,
    )
  ) {
    notice("Ingresá entre 0,001 y 1000 kilos, con hasta tres decimales.", true);
    return;
  }
  state.busy = true;
  $("calculate").disabled = true;
  $("calculate").textContent = "Comparando precios…";
  const revision = state.revision;
  try {
    const results = await Promise.all(
      state.stores.map(async (store) => {
        // Incomplete baskets must never look cheaper than complete ones.
        const published = state.latest.find((s) => s.localId === store.id);
        if (
          request.items.some(
            (i) => !published?.productos.some((p) => p.id === i.productoId),
          )
        )
          return { store, incomplete: true };
        return {
          store,
          quote: await api("/api/presupuestos/calcular", {
            method: "POST",
            body: JSON.stringify({ ...request, localId: store.id }),
          }),
        };
      }),
    );
    if (revision !== state.revision) return;
    results.sort(
      (a, b) => (a.quote?.total ?? Infinity) - (b.quote?.total ?? Infinity),
    );
    state.quotes = results;
    state.selected = results.find((r) => r.quote)?.quote || null;
    state.pdf = null;
    renderStores();
    renderQuote();
    if (!state.selected)
      notice(
        "Ningún local tiene precio para todos los cortes elegidos. Probá con otra lista.",
        true,
      );
  } catch (error) {
    invalidate();
    notice(error.message, true);
  } finally {
    state.busy = false;
    $("calculate").disabled = false;
    $("calculate").textContent = "Calcular y comparar locales";
  }
}
function renderStores() {
  $("store-results").innerHTML = state.quotes
    .map(
      (r, index) =>
        `<button class="store-option ${state.selected?.localId === r.store.id ? "selected" : ""}" data-store="${r.store.id}" ${r.incomplete ? "disabled" : ""} aria-pressed="${state.selected?.localId === r.store.id}"><span>${escape(r.store.nombre)}<small>${r.incomplete ? "Faltan precios para tu lista" : index === 0 ? "Menor total para tu lista completa" : escape(r.store.direccion)}</small></span><strong>${r.quote ? money(r.quote.total) : "Sin total"}</strong></button>`,
    )
    .join("");
}
function summary(q) {
  return `Mi asadito en ${q.localNombre}\n${q.direccion}\n${q.items.map((i) => `${i.nombre}: ${i.cantidad} kg x ${money(i.precio)} = ${money(i.subtotal)} (precio del ${date(i.fecha)})`).join("\n")}\nTOTAL ESTIMADO: ${money(q.total)} ARS\nPresupuesto generado el ${date(q.generado)}. Confirmar precio y disponibilidad con el local.`;
}
function renderQuote() {
  const q = state.selected;
  $("quote-store").textContent = q?.localNombre || "Todavía sin local";
  $("quote-address").textContent =
    q?.direccion || "Elegí tus kilos y calculá para empezar.";
  $("quote-total").textContent = q ? money(q.total) : "$ —";
  $("quote-lines").innerHTML = q
    ? q.items
        .map(
          (i) =>
            `<div class="quote-line"><span>${escape(i.nombre)}<small>${i.cantidad} kg × ${money(i.precio)} · ${date(i.fecha)}</small></span><strong>${money(i.subtotal)}</strong></div>`,
        )
        .join("")
    : '<p class="empty">Tu lista empieza con un buen corte.</p>';
  $("download-pdf").disabled = !q;
  $("share-pdf").hidden = !state.pdf;
  $("whatsapp").hidden = !q;
  if (q)
    $("whatsapp").href =
      `https://wa.me/?text=${encodeURIComponent(summary(q))}`;
  $("report-help").textContent = state.user.authenticated
    ? "El PDF usa los precios vigentes del servidor al generarse."
    : "Iniciá sesión para descargar tu presupuesto en PDF.";
}
function renderSession() {
  const logged = state.user.authenticated;
  $("session-actions").innerHTML = logged
    ? `<span class="session-name" title="${escape(state.user.email)}">${escape(state.user.email)}</span><button id="logout" class="button secondary small">Salir</button>`
    : '<button class="button small" id="open-login">Ingresar</button>';
  $("admin-link").hidden = !state.user.admin;
  $("gestion").hidden = !state.user.admin;
  renderQuote();
}
function openAuth() {
  state.register = false;
  renderAuth();
  $("auth-dialog").showModal();
}
function renderAuth() {
  $("auth-title").textContent = state.register
    ? "Creá tu cuenta"
    : "Ingresá a tu cuenta";
  $("auth-description").textContent = state.register
    ? "Usá una contraseña de al menos 10 caracteres (máximo 72 bytes)."
    : "Descargá tu presupuesto y llevalo a la juntada.";
  $("auth-submit").textContent = state.register ? "Crear cuenta" : "Ingresar";
  $("auth-toggle").textContent = state.register
    ? "Ya tengo cuenta. Ingresar"
    : "¿Primera vez? Creá tu cuenta";
  $("auth-password").autocomplete = state.register
    ? "new-password"
    : "current-password";
  $("auth-password").minLength = state.register ? 10 : 1;
  $("auth-error").textContent = "";
}
$("auth-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("auth-submit").disabled = true;
  const values = Object.fromEntries(new FormData(event.target));
  try {
    if (state.register) {
      await api("/api/auth/registro", {
        method: "POST",
        body: JSON.stringify(values),
      });
      state.register = false;
      renderAuth();
      $("auth-form").reset();
      $("auth-description").textContent =
        "Cuenta creada. Ingresá con tu email y contraseña.";
    } else {
      await api("/api/auth/login", {
        method: "POST",
        body: new URLSearchParams(values),
      });
      await refreshCsrf();
      state.user = await api("/api/auth/me");
      renderSession();
      renderAdmin();
      $("auth-dialog").close();
      $("auth-form").reset();
      notice("Sesión iniciada. Ya podés descargar tu PDF.");
    }
  } catch (error) {
    $("auth-error").textContent = error.message;
  } finally {
    $("auth-submit").disabled = false;
  }
});
$("auth-toggle").onclick = () => {
  state.register = !state.register;
  renderAuth();
};
$("session-actions").onclick = async (event) => {
  if (event.target.id === "open-login") openAuth();
  if (event.target.id === "logout") {
    try {
      await api("/api/auth/logout", { method: "POST" });
      state.user = {};
      state.pdf = null;
      await refreshCsrf();
      renderSession();
      notice("Sesión cerrada.");
    } catch (error) {
      notice(error.message, true);
    }
  }
};
$("download-pdf").onclick = async () => {
  if (!state.user.authenticated) {
    openAuth();
    return;
  }
  const revision = state.revision,
    selected = state.selected?.localId;
  $("download-pdf").disabled = true;
  try {
    const blob = await api("/api/presupuestos/pdf", {
      method: "POST",
      body: JSON.stringify(payload()),
      pdf: true,
    });
    if (revision !== state.revision || selected !== state.selected?.localId)
      return;
    state.pdf = new File([blob], "presupuesto-asadito.pdf", {
      type: "application/pdf",
    });
    const url = URL.createObjectURL(blob),
      link = document.createElement("a");
    link.href = url;
    link.download = "presupuesto-asadito.pdf";
    document.body.append(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 10000);
    renderQuote();
    notice(
      "PDF generado. Podés adjuntarlo en WhatsApp o usar el menú para compartir.",
    );
  } catch (error) {
    notice(error.message, true);
  } finally {
    $("download-pdf").disabled = !state.selected;
  }
};
$("share-pdf").onclick = async () => {
  if (!state.pdf) return;
  try {
    if (navigator.canShare?.({ files: [state.pdf] }))
      await navigator.share({
        files: [state.pdf],
        title: "Mi presupuesto de Asadito RG",
      });
    else
      notice(
        "Este navegador no permite compartir archivos. Adjuntá el PDF descargado en la conversación de WhatsApp.",
      );
  } catch (error) {
    if (error.name !== "AbortError")
      notice(
        "No se pudo abrir el menú para compartir. Adjuntá el PDF descargado.",
        true,
      );
  }
};
$("basket").oninput = (event) => {
  const id = event.target.dataset.product;
  if (!id) return;
  state.quantities[id] =
    event.target.value === "" ? 0 : Number(event.target.value);
  invalidate();
};
$("calculate").onclick = calculate;
$("clear-basket").onclick = () => {
  state.quantities = {};
  invalidate();
  renderBasket();
};
$("search").oninput = renderComparison;
$("refresh").onclick = load;
$("store-results").onclick = (event) => {
  const button = event.target.closest("[data-store]");
  if (!button) return;
  state.selected = state.quotes.find(
    (r) => r.store.id === +button.dataset.store,
  )?.quote;
  state.pdf = null;
  renderStores();
  renderQuote();
};
document
  .querySelectorAll("[data-close]")
  .forEach(
    (button) => (button.onclick = () => $(button.dataset.close).close()),
  );

// The server checks ADMIN independently of this UI.
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
  if (!state.user.admin) {
    $("admin-list").replaceChildren();
    return;
  }
  const kind = $("admin-kind").value;
  $("admin-list").innerHTML =
    `<table><thead><tr><th>Registro</th><th>Detalle</th><th>Acciones</th></tr></thead><tbody>${adminRows()
      .map(
        (r) =>
          `<tr><th scope="row">${escape(r.nombre || r.producto.nombre)}</th><td>${kind === "precios" ? `${escape(r.local.nombre)} · ${money(r.precio)} · ${date(r.fecha)}` : escape(r.direccion || "Corte por kilo")}</td><td><div class="admin-actions"><button class="button secondary small" data-edit="${r.id}">Editar</button><button class="text-button" data-delete="${r.id}">Dar de baja</button></div></td></tr>`,
      )
      .join(
        "",
      )}</tbody></table>${!adminRows().length ? '<p class="empty">Sin registros activos.</p>' : ""}`;
}
function field(name, label, type, value = "", attrs = "") {
  return `<label>${label}<input name="${name}" type="${type}" value="${escape(value)}" ${attrs} required></label>`;
}
function edit(id = null) {
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
  if (button.dataset.delete) {
    deleting = { kind: $("admin-kind").value, id: +button.dataset.delete };
    $("delete-error").textContent = "";
    $("delete-dialog").showModal();
  }
};
$("admin-form").onsubmit = async (event) => {
  event.preventDefault();
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
      "Registro guardado. Volvé a calcular tu lista para usar los nuevos precios.",
    );
  } catch (error) {
    $("admin-error").textContent = error.message;
  } finally {
    submit.disabled = false;
  }
};
$("confirm-delete").onclick = async () => {
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
async function init() {
  try {
    await refreshCsrf();
    state.user = await api("/api/auth/me");
    renderSession();
  } catch (error) {
    notice(error.message, true);
  }
  await load();
}
init();
