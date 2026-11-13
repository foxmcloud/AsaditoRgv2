// El reporte es una presentación del presupuesto del navegador, no un comprobante de compra.
import { money, date } from "./session.js";
let recursos;
async function cargarRecursos() {
  if (!recursos)
    recursos = (async () => {
      if (!window.jspdf)
        await new Promise((resolve, reject) => {
          const script = document.createElement("script");
          script.src = "/assets/vendor/jspdf/jspdf.umd.min.js";
          script.onload = resolve;
          script.onerror = () =>
            reject(new Error("No se pudo cargar la biblioteca PDF"));
          document.head.append(script);
        });
      const archivos = await Promise.all(
        [
          "/assets/fonts/LiberationSans-Regular.ttf",
          "/assets/fonts/LiberationSans-Bold.ttf",
          "/assets/fonts/LiberationSerif-Regular.ttf",
          "/assets/img/Logo%20Asadito%20RG%20-%20PNG.png",
        ].map(async (url) => {
          const respuesta = await fetch(url);
          if (!respuesta.ok)
            throw new Error("No se pudo cargar un recurso del PDF");
          return new Uint8Array(await respuesta.arrayBuffer());
        }),
      );
      return archivos;
    })().catch((error) => {
      recursos = null;
      throw error;
    });
  return recursos;
}
function base64(bytes) {
  let texto = "";
  for (let i = 0; i < bytes.length; i += 8192)
    texto += String.fromCharCode(...bytes.subarray(i, i + 8192));
  return btoa(texto);
}
export async function generarPDF(presupuesto) {
  const [regular, negrita, titulo, logo] = await cargarRecursos();
  const doc = new window.jspdf.jsPDF({
    unit: "pt",
    format: "a4",
    compress: true,
    putOnlyUsedFonts: true,
  });
  for (const [nombre, fuente, estilo, datos] of [
    ["regular.ttf", "Texto", "normal", regular],
    ["bold.ttf", "Texto", "bold", negrita],
    ["titulo.ttf", "Titulo", "normal", titulo],
  ]) {
    doc.addFileToVFS(nombre, base64(datos));
    doc.addFont(nombre, fuente, estilo);
  }
  doc.setProperties({
    title: "Presupuesto de asado - " + presupuesto.localNombre,
    author: "Asadito RG",
  });
  const ancho = doc.internal.pageSize.getWidth(),
    alto = doc.internal.pageSize.getHeight(),
    margen = 44,
    contenido = ancho - margen * 2;
  const colores = {
    tinta: [41, 42, 36],
    gris: [100, 103, 94],
    naranja: [169, 64, 36],
    verde: [51, 92, 72],
    crema: [250, 247, 240],
    linea: [228, 223, 210],
    blanco: [255, 255, 255],
  };
  let y;
  function texto(
    valor,
    x,
    altura,
    tamano = 10,
    estilo = "normal",
    color = "tinta",
    opciones = {},
  ) {
    doc.setFont("Texto", estilo);
    doc.setFontSize(tamano);
    doc.setTextColor(...colores[color]);
    doc.text(String(valor), x, altura, opciones);
  }
  function caja(x, altura, w, h, color, radio = 0) {
    doc.setFillColor(...colores[color]);
    if (radio) doc.roundedRect(x, altura, w, h, radio, radio, "F");
    else doc.rect(x, altura, w, h, "F");
  }
  function pagina(continuacion = false) {
    if (continuacion) doc.addPage();
    caja(0, 0, ancho, 187, "crema");
    caja(0, 0, ancho, 6, "naranja");
    doc.addImage(logo, "PNG", margen - 26, 11.4, 234, (234 * 897) / 2481);
    texto("LA JUNTADA EMPIEZA ACÁ", margen, 86, 7.3, "bold", "gris");
    texto(
      "Río Grande · Tierra del Fuego",
      ancho - margen,
      53,
      9,
      "normal",
      "gris",
      { align: "right" },
    );
    texto(
      presupuesto.guardado ? `PRESUPUESTO GUARDADO #${presupuesto.presupuestoId}` : "PRESUPUESTO ESTIMATIVO",
      ancho - margen,
      72,
      7.5,
      "bold",
      "naranja",
      { align: "right" },
    );
    doc.setFont("Titulo", "normal");
    doc.setFontSize(continuacion ? 29 : 34);
    doc.setTextColor(...colores.tinta);
    doc.text(
      continuacion ? "Tu presupuesto, en detalle." : "Tu asado, en números.",
      margen,
      122,
    );
    texto(
      (presupuesto.guardado ? "Guardado el " : "Generado el ") +
        date(presupuesto.generado) +
        "   /   Moneda: ARS · Pesos argentinos",
      margen,
      145,
      9,
      "normal",
      "gris",
    );
    if (presupuesto.guardado) texto("PDF regenerado el " + date(presupuesto.regenerado), margen, 160, 8, "normal", "gris");
    doc.setFont("Texto", "bold");
    doc.setFontSize(13);
    const nombre = doc.splitTextToSize(presupuesto.localNombre, contenido - 34);
    doc.setFont("Texto", "normal");
    doc.setFontSize(9);
    const direccion = doc.splitTextToSize(
      presupuesto.direccion,
      contenido - 34,
    );
    const altura = 45 + nombre.length * 16 + direccion.length * 12;
    caja(margen, 175, contenido, altura, "blanco", 9);
    texto("TU COMERCIO", margen + 16, 195, 7, "bold", "naranja");
    y = 215;
    for (const linea of nombre) {
      texto(linea, margen + 16, y, 13, "bold");
      y += 16;
    }
    for (const linea of direccion) {
      texto(linea, margen + 16, y + 1, 9, "normal", "gris");
      y += 12;
    }
    y = 175 + altura + 24;
  }
  function encabezado() {
    caja(margen, y, contenido, 30, "verde", 5);
    texto(
      presupuesto.guardado ? "CORTE / PRECIO GUARDADO" : "CORTE / FECHA DEL PRECIO",
      margen + 12,
      y + 19,
      7.5,
      "bold",
      "blanco",
    );
    for (const [label, x] of [
      ["KG", margen + 246],
      ["PRECIO / KG", margen + 358],
      ["SUBTOTAL", ancho - margen - 12],
    ])
      texto(label, x, y + 19, 8, "bold", "blanco", { align: "right" });
    y += 30;
  }
  pagina();
  encabezado();
  for (const [indice, item] of presupuesto.items.entries()) {
    doc.setFont("Texto", "bold");
    doc.setFontSize(10);
    const nombre = doc.splitTextToSize(item.nombre, 178),
      altura = Math.max(55, 31 + nombre.length * 13);
    if (y + altura > alto - 85) {
      pagina(true);
      encabezado();
    }
    if (indice % 2 === 0) caja(margen, y, contenido, altura, "crema");
    nombre.forEach((linea, i) =>
      texto(linea, margen + 12, y + 20 + i * 13, 10, "bold"),
    );
    texto(
      item.fecha ? "Precio del " + date(item.fecha) : "Importe guardado en el presupuesto",
      margen + 12,
      y + 22 + nombre.length * 13,
      8,
      "normal",
      "gris",
    );
    texto(
      String(item.cantidad).replace(".", ","),
      margen + 246,
      y + 22,
      10,
      "normal",
      "tinta",
      { align: "right" },
    );
    texto(money(item.precio), margen + 358, y + 22, 9.5, "normal", "tinta", {
      align: "right",
    });
    texto(
      money(item.subtotal),
      ancho - margen - 12,
      y + 22,
      9.5,
      "bold",
      "tinta",
      { align: "right" },
    );
    y += altura;
    doc.setDrawColor(...colores.linea);
    doc.line(margen, y, ancho - margen, y);
  }
  if (y + 163 > alto - 85) pagina(true);
  y += 20;
  caja(margen, y, contenido, 81, "verde", 9);
  texto(presupuesto.guardado ? "TOTAL GUARDADO" : "TOTAL ESTIMADO", margen + 17, y + 25, 9, "bold", "blanco");
  texto(
    `${presupuesto.items.length} ${presupuesto.items.length === 1 ? "corte" : "cortes"} · ${String(presupuesto.items.reduce((suma, i) => suma + i.cantidad, 0)).replace(".", ",")} kg en total`,
    margen + 17,
    y + 45,
    9,
    "normal",
    "blanco",
  );
  texto("Pesos argentinos · ARS", margen + 17, y + 64, 8, "normal", "blanco");
  doc.setFont("Texto", "bold");
  let tamano = 25;
  doc.setFontSize(tamano);
  while (doc.getTextWidth(money(presupuesto.total)) > 265 && tamano > 12)
    doc.setFontSize((tamano -= 0.5));
  texto(
    money(presupuesto.total),
    ancho - margen - 17,
    y + 47,
    tamano,
    "bold",
    "blanco",
    { align: "right" },
  );
  texto("ANTES DE ENCENDER EL FUEGO", margen, y + 102, 8, "bold", "naranja");
  texto(
    "Confirmá vigencia y disponibilidad con el comercio. Los importes no incluyen envío.",
    margen,
    y + 119,
    8.5,
    "normal",
    "gris",
  );
  texto(
    "Este presupuesto es orientativo y no constituye una reserva ni un comprobante de compra.",
    margen,
    y + 133,
    8.5,
    "normal",
    "gris",
  );
  const paginas = doc.getNumberOfPages();
  for (let numero = 1; numero <= paginas; numero++) {
    doc.setPage(numero);
    doc.setDrawColor(...colores.linea);
    doc.line(margen, alto - 57, ancho - margen, alto - 57);
    doc.addImage(
      logo,
      "PNG",
      margen - 11,
      alto - 58.2,
      100,
      (100 * 897) / 2481,
    );
    texto(
      "Compará. Calculá. Compartí.",
      margen + 100,
      alto - 39,
      8,
      "normal",
      "gris",
    );
    texto(
      `Página ${numero} de ${paginas}`,
      ancho - margen,
      alto - 39,
      8,
      "normal",
      "gris",
      { align: "right" },
    );
  }
  return doc.output("blob");
}
