// Adaptación de los datos históricos; no consulta precios ni recalcula importes.
function importe(valor, campo) {
  if (valor == null || !["number", "string"].includes(typeof valor) ||
      String(valor).trim() === "" || !Number.isFinite(Number(valor)) || Number(valor) < 0)
    throw new Error(`El presupuesto no incluye un ${campo} válido.`);
  return Number(valor);
}
export function fechaGuardada(valor) {
  if (!valor) return null;
  const texto = String(valor);
  if (/^\d{4}-\d{2}-\d{2}$/.test(texto)) {
    const fecha = new Date(`${texto}T12:00:00Z`);
    return !Number.isNaN(fecha.getTime()) && fecha.toISOString().slice(0, 10) === texto ? texto : null;
  }
  // LocalDateTime no contiene zona: conservar su fecha de calendario.
  if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/.test(texto))
    return fechaGuardada(texto.slice(0, 10));
  const fecha = new Date(texto);
  if (Number.isNaN(fecha.getTime())) return null;
  const partes = new Intl.DateTimeFormat("es-AR", {
    year: "numeric", month: "2-digit", day: "2-digit", timeZone: "America/Argentina/Buenos_Aires",
  }).formatToParts(fecha);
  const valorParte = tipo => partes.find(parte => parte.type === tipo).value;
  return `${valorParte("year")}-${valorParte("month")}-${valorParte("day")}`;
}
export function cabeceraGuardada(datos) {
  const id = Number(datos?.id ?? datos?.presupuestoId);
  if (!Number.isSafeInteger(id) || id <= 0) throw new Error("Hay un presupuesto sin número válido.");
  const moneda = String(datos.moneda || "ARS").toUpperCase();
  if (moneda !== "ARS") throw new Error("Este reporte admite presupuestos en pesos argentinos (ARS).");
  return {
    id, moneda, fecha: fechaGuardada(datos.fecha), total: importe(datos.total, "total"),
    localNombre: String(datos.localNombre ?? datos.localnombre ?? datos.local?.nombre ?? "Comercio"),
    localDireccion: String(datos.localDireccion ?? datos.localdireccion ?? datos.local?.direccion ?? ""),
  };
}
export function listadoGuardado(respuesta, paginaSolicitada = 0) {
  const filas = Array.isArray(respuesta) ? respuesta : respuesta?.content;
  if (!Array.isArray(filas)) throw new Error("No se pudo interpretar el listado de presupuestos.");
  const paginado = !Array.isArray(respuesta);
  const pagina = paginado ? Number(respuesta.number ?? paginaSolicitada) : 0;
  const paginas = paginado ? Math.max(1, Number(respuesta.totalPages ?? 1)) : 1;
  if (!Number.isSafeInteger(pagina) || pagina < 0 || !Number.isSafeInteger(paginas) || paginas < 1)
    throw new Error("La paginación del historial no es válida.");
  return { filas: filas.map(cabeceraGuardada), pagina, paginas, paginado };
}
export function adaptarPresupuestoGuardado(datos, ahora = new Date().toISOString()) {
  const p = cabeceraGuardada(datos);
  if (!p.fecha) throw new Error("El presupuesto no incluye una fecha válida.");
  if (!Array.isArray(datos.detalles) || !datos.detalles.length)
    throw new Error("El presupuesto no contiene cortes guardados.");
  return {
    guardado: true, presupuestoId: p.id, localNombre: p.localNombre,
    direccion: p.localDireccion, total: p.total, moneda: p.moneda,
    generado: p.fecha, regenerado: fechaGuardada(ahora),
    items: datos.detalles.map(detalle => {
      const cantidad = importe(detalle.cantidad, "peso");
      if (cantidad <= 0) throw new Error("Hay un corte sin cantidad válida.");
      return {
        nombre: String(detalle.corteNombre || "Corte"), cantidad,
        precio: importe(detalle.precioKg, "precio por kilo"),
        subtotal: importe(detalle.subtotal, "subtotal"),
        // El controlador actual no devuelve fechaPrecio: no inventar una.
        fecha: fechaGuardada(detalle.fechaPrecio),
      };
    }),
  };
}
