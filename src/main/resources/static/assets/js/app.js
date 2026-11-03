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
const state = {
  products: [],
  stores: [],
  prices: [],
  latest: [],
  quantities: {},
  quotes: [],
  selected: null,
  pdf: null,
  revision: 0,
  busy: false,
};
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
      '<div class="empty"><strong>Todavía no hay precios publicados.</strong>Los locales y cortes ya están disponibles. Un operador debe cargar los precios para comenzar a comparar.</div>';
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
  $("report-help").textContent = session.user.authenticated
    ? "El PDF usa los precios vigentes del servidor al generarse."
    : "Iniciá sesión para descargar tu presupuesto en PDF.";
}
$("download-pdf").onclick = async () => {
  if (!session.user.authenticated) {
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
onSessionChange(() => {
  if (!session.user.authenticated) state.pdf = null;
  renderQuote();
});
async function init() {
  await initSession();
  await load();
}
init();
