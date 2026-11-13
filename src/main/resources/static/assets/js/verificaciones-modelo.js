import { fechaGuardada } from "./presupuesto-guardado.js";
export const valoraciones = {
  ACORDE: { titulo: "Acorde a lo estimado", detalle: "Lo que pagaste está dentro del margen del 5 %.", clase: "acorde" },
  SOBREESTIMADO: { titulo: "Se estimó de más", detalle: "Pagaste menos de lo que habías presupuestado.", clase: "sobreestimado" },
  SUBESTIMADO: { titulo: "Se estimó de menos", detalle: "Pagaste más de lo que habías presupuestado.", clase: "subestimado" },
};
export function normalizarVerificacion(datos, id) {
  if (!datos || Number(datos.presupuestoId) !== Number(id) || !valoraciones[datos.valoracion])
    throw new Error("La verificación recibida no corresponde al presupuesto.");
  const salida = { presupuestoId: Number(id), valoracion: datos.valoracion,
    fecha: fechaGuardada(datos.fecha), observacion: String(datos.observacion || "") };
  for (const campo of ["totalEstimado", "totalReal", "diferencia", "porcentaje"]) {
    if (datos[campo] == null || String(datos[campo]).trim() === "" || !Number.isFinite(Number(datos[campo])))
      throw new Error("La verificación contiene importes incompletos.");
    salida[campo] = Number(datos[campo]);
  }
  if (salida.totalReal <= 0 || salida.totalEstimado <= 0)
    throw new Error("La verificación contiene un total inválido.");
  return salida;
}
export function resumirResultados(filas) {
  const verificadas = filas.filter(fila => fila.verificacion);
  return {
    verificadas: verificadas.length,
    pendientes: filas.filter(fila => fila.estado === "pendiente").length,
    errores: filas.filter(fila => fila.estado === "error").length,
    acordes: verificadas.filter(fila => fila.verificacion.valoracion === "ACORDE").length,
    estimado: verificadas.reduce((suma, fila) => suma + fila.verificacion.totalEstimado, 0),
    pagado: verificadas.reduce((suma, fila) => suma + fila.verificacion.totalReal, 0),
    diferencia: verificadas.reduce((suma, fila) => suma + fila.verificacion.diferencia, 0),
  };
}
