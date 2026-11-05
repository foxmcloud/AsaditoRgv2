export const $ = (id) => document.getElementById(id);
export const money = (value) =>
  new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" }).format(
    value,
  );
export const date = (value) =>
  value
    ? new Intl.DateTimeFormat("es-AR").format(new Date(`${value}T12:00:00`))
    : "Sin fecha";

export const session = { user: {}, csrf: null, register: false };
let onChange = () => {};
export function onSessionChange(callback) {
  onChange = callback;
}
export const escape = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
export function notice(message, error = false) {
  $("notice").textContent = message;
  $("notice").className = error ? "error" : "";
  $("notice").hidden = false;
}
export async function refreshCsrf() {
  const response = await fetch("/api/auth/csrf", {
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok)
    throw new Error("No se pudo iniciar la sesión. Recargá la página.");
  session.csrf = await response.json();
}
export async function api(path, options = {}) {
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof URLSearchParams))
    headers.set("Content-Type", "application/json");
  if (options.method && options.method !== "GET") {
    if (!session.csrf) await refreshCsrf();
    headers.set(session.csrf.headerName, session.csrf.token);
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
      session.user = {};
      renderSession();
    }
    if (response.status === 403) {
      session.csrf = null;
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
function renderSession() {
  const logged = session.user.authenticated;
  $("session-actions").innerHTML = logged
    ? `<span class="session-name" title="${escape(session.user.email)}">${escape(session.user.email)}</span><button id="logout" class="button secondary small">Salir</button>`
    : '<button class="button small" id="open-login">Ingresar</button>';
  const link = $("admin-link");
  if (link) {
    link.hidden = !(session.user.admin || session.user.operador);
    link.textContent = session.user.operador
      ? "Panel de operador"
      : "Administrar catálogo";
  }
  onChange();
}
export function openAuth() {
  session.register = false;
  renderAuth();
  $("auth-dialog").showModal();
}
function renderAuth() {
  $("auth-title").textContent = session.register
    ? "Creá tu cuenta"
    : "Ingresá a tu cuenta";
  $("auth-description").textContent = session.register
    ? "Usá una contraseña de al menos 10 caracteres (máximo 72 bytes)."
    : $("panel-access")
      ? "Usá tu cuenta de operador o administrador."
      : "Descargá tu presupuesto y llevalo a la juntada.";
  $("auth-submit").textContent = session.register ? "Crear cuenta" : "Ingresar";
  $("auth-toggle").textContent = session.register
    ? "Ya tengo cuenta. Ingresar"
    : "¿Primera vez? Creá tu cuenta";
  $("auth-password").autocomplete = session.register
    ? "new-password"
    : "current-password";
  $("auth-password").minLength = session.register ? 10 : 1;
  $("auth-error").textContent = "";
}
$("auth-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("auth-submit").disabled = true;
  const values = Object.fromEntries(new FormData(event.target));
  try {
    if (session.register) {
      await api("/api/auth/registro", {
        method: "POST",
        body: JSON.stringify(values),
      });
      session.register = false;
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
      session.user = await api("/api/auth/me");
      renderSession();
      $("auth-dialog").close();
      $("auth-form").reset();
      notice("Sesión iniciada.");
    }
  } catch (error) {
    $("auth-error").textContent = error.message;
  } finally {
    $("auth-submit").disabled = false;
  }
});
$("auth-toggle").onclick = () => {
  session.register = !session.register;
  renderAuth();
};
$("session-actions").onclick = async (event) => {
  if (event.target.id === "open-login") openAuth();
  if (event.target.id === "logout") {
    try {
      await api("/api/auth/logout", { method: "POST" });
      session.user = {};
      await refreshCsrf();
      renderSession();
      notice("Sesión cerrada.");
    } catch (error) {
      notice(error.message, true);
    }
  }
};
document.querySelectorAll("[data-close]").forEach((button) => {
  button.onclick = () => $(button.dataset.close).close();
});
export async function initSession() {
  try {
    await refreshCsrf();
    session.user = await api("/api/auth/me");
    renderSession();
  } catch (error) {
    notice(error.message, true);
  }
}
