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
const notification = $("notice");
let noticeTimeout;
let noticeIsError = false;
function dismissNotice() {
  clearTimeout(noticeTimeout);
  notification.hidden = true;
}
function scheduleNoticeDismissal() {
  clearTimeout(noticeTimeout);
  if (
    !noticeIsError &&
    !notification.hidden &&
    !notification.matches(":hover") &&
    !notification.contains(document.activeElement)
  ) {
    noticeTimeout = setTimeout(dismissNotice, 5000);
  }
}
notification.addEventListener("pointerenter", () =>
  clearTimeout(noticeTimeout),
);
notification.addEventListener("pointerleave", scheduleNoticeDismissal);
notification.addEventListener("focusin", () => clearTimeout(noticeTimeout));
notification.addEventListener("focusout", scheduleNoticeDismissal);
export function notice(message, error = false) {
  clearTimeout(noticeTimeout);
  noticeIsError = error;
  const icon = document.createElement("span");
  icon.className = "toast-icon";
  icon.setAttribute("aria-hidden", "true");
  icon.textContent = error ? "!" : "✓";
  const content = document.createElement("div");
  content.className = "toast-content";
  const title = document.createElement("strong");
  title.textContent = error ? "No se pudo completar" : "Listo";
  const text = document.createElement("span");
  text.textContent = message;
  content.append(title, text);
  const close = document.createElement("button");
  close.type = "button";
  close.className = "toast-close";
  close.setAttribute("aria-label", "Cerrar notificación");
  close.textContent = "×";
  close.onclick = dismissNotice;
  notification.replaceChildren(icon, content, close);
  notification.className = error ? "toast error" : "toast";
  notification.hidden = false;
  scheduleNoticeDismissal();
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
  // Como en el PDF: enviar campos de formulario, no DTO de entrada.
  if (typeof options.body === "string")
    options = {
      ...options,
      body: new URLSearchParams(JSON.parse(options.body)),
    };
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
    if (path === "/api/login" && response.status === 401)
      throw new Error("Email o contraseña incorrectos");
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
        body.code ? `Código: ${body.code}` : "",
        body.reference ? `Referencia: ${body.reference}` : "",
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
      await api("/api/login", {
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
      await api("/api/logout", { method: "POST" });
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
