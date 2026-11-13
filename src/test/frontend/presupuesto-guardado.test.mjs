import assert from "node:assert/strict";
import test from "node:test";
import { adaptarPresupuestoGuardado, fechaGuardada, listadoGuardado } from "../../main/resources/static/assets/js/presupuesto-guardado.js";
const presupuesto = {
  id: 41, fecha: "2026-11-13T01:30:00Z", localNombre: "Local histórico", localDireccion: "Calle 123", moneda: "ARS", total: "17777.77",
  detalles: [{ id: 99, corteNombre: "Vacío", cantidad: "1.5", precioKg: "12000", subtotal: "17777.77" }],
};
test("mapea el contrato del controlador sin recalcular los importes guardados", () => {
  const p = adaptarPresupuestoGuardado(presupuesto, "2026-11-14T12:00:00Z");
  assert.equal(p.total, 17777.77);
  assert.equal(p.items[0].subtotal, 17777.77);
  assert.equal(p.items[0].precio, 12000);
  assert.equal(p.items[0].cantidad, 1.5);
  assert.equal(p.items[0].fecha, null);
  assert.equal(p.items[0].productoId, undefined); // El ID del detalle no es el del corte.
  assert.equal(p.generado, "2026-11-12");
  assert.equal(p.regenerado, "2026-11-14");
  assert.equal(p.presupuestoId, 41);
  assert.equal(p.direccion, "Calle 123");
});
test("conserva fechas de calendario y convierte instantes al horario argentino", () => {
  assert.equal(fechaGuardada("2026-11-13"), "2026-11-13");
  assert.equal(fechaGuardada("2026-11-13T01:30:00"), "2026-11-13");
  assert.equal(fechaGuardada("2026-11-13T01:30:00Z"), "2026-11-12");
  assert.equal(fechaGuardada("2026-02-31"), null);
  assert.equal(fechaGuardada("mal"), null);
});
test("acepta listado simple y Page de Spring sin perder la paginación", () => {
  assert.equal(listadoGuardado([presupuesto]).filas[0].id, 41);
  const p = listadoGuardado({ content: [presupuesto], number: 1, totalPages: 3 });
  assert.equal(p.pagina, 1); assert.equal(p.paginas, 3); assert.equal(p.paginado, true);
  assert.equal(listadoGuardado({ content: [], number: 0, totalPages: 0 }).paginas, 1);
  assert.throws(() => listadoGuardado({}));
});
test("rechaza datos incompletos sin inventar precios o fechas", () => {
  for (const total of [null, "", "infinito", -1, Infinity])
    assert.throws(() => adaptarPresupuestoGuardado({ ...presupuesto, total }));
  assert.throws(() => adaptarPresupuestoGuardado({ ...presupuesto, detalles: [] }));
  assert.throws(() => adaptarPresupuestoGuardado({ ...presupuesto, fecha: null }));
  assert.throws(() => adaptarPresupuestoGuardado({ ...presupuesto, moneda: "USD" }));
  assert.throws(() => adaptarPresupuestoGuardado({ ...presupuesto, detalles: [{ ...presupuesto.detalles[0], precioKg: null }] }));
});
