// Lógica de presentación: no guarda datos ni concede permisos.
export function ultimosPrecios(precios, productos, locales) {
  const grupos = new Map();
  for (const precio of precios) {
    const local = locales.find((l) => l.id === precio.local?.id);
    const producto = productos.find((p) => p.id === precio.producto?.id);
    if (!local || !producto || !(Number(precio.precio) > 0)) continue;
    if (!grupos.has(local.id))
      grupos.set(local.id, {
        localId: local.id,
        localNombre: local.nombre,
        direccion: local.direccion,
        productos: [],
      });
    const lista = grupos.get(local.id).productos;
    const anterior = lista.find((p) => p.id === producto.id);
    if (
      !anterior ||
      precio.fecha > anterior.fecha ||
      (precio.fecha === anterior.fecha && precio.id > anterior.precioId)
    ) {
      if (anterior) lista.splice(lista.indexOf(anterior), 1);
      lista.push({
        id: producto.id,
        nombre: producto.nombre,
        precio: Number(precio.precio),
        fecha: precio.fecha,
        precioId: precio.id,
      });
    }
  }
  return [...grupos.values()];
}
export function calcularPresupuesto(local, publicados, items) {
  if (
    !items.length ||
    items.length > 100 ||
    items.some(
      (i) =>
        !Number.isFinite(i.cantidad) ||
        i.cantidad <= 0 ||
        i.cantidad > 1000 ||
        !Number.isInteger(i.cantidad * 2),
    )
  )
    throw new Error("Elegí cantidades de 0,5 kg en 0,5 kg, hasta 1000 kg.");
  let totalCentavos = 0;
  const detalle = items.map((item) => {
    const precio = publicados?.productos.find((p) => p.id === item.productoId);
    if (!precio) throw new Error("Faltan precios para completar la lista.");
    const centavos = Math.round(
      Math.round(precio.precio * 100) * item.cantidad,
    );
    totalCentavos += centavos;
    if (!Number.isSafeInteger(totalCentavos))
      throw new Error(
        "El presupuesto supera el importe que puede calcularse con precisión.",
      );
    return {
      productoId: item.productoId,
      nombre: precio.nombre,
      cantidad: item.cantidad,
      precio: precio.precio,
      fecha: precio.fecha,
      subtotal: centavos / 100,
    };
  });
  const hoy = new Date(Date.now() - new Date().getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 10);
  return {
    localId: local.id,
    localNombre: local.nombre,
    direccion: local.direccion,
    items: detalle,
    total: totalCentavos / 100,
    moneda: "ARS",
    generado: hoy,
  };
}
