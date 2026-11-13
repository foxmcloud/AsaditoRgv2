import test from "node:test";
import assert from "node:assert/strict";
import { normalizarVerificacion, resumirResultados } from "../../main/resources/static/assets/js/verificaciones-modelo.js";
const ejemplo = { presupuestoId: 41, totalEstimado: 20000, totalReal: 22000, diferencia: 2000, porcentaje: 10, valoracion: "SUBESTIMADO", fecha: "2026-11-13T20:00:00", observacion: "Compra real" };
test("usa los importes y la valoración que devuelve el servidor", () => {
  const v = normalizarVerificacion(ejemplo, 41);
  assert.equal(v.totalReal, 22000); assert.equal(v.valoracion, "SUBESTIMADO");
  assert.equal(v.fecha, "2026-11-13"); assert.equal(v.observacion, "Compra real");
});
test("no confunde errores con verificaciones pendientes", () => {
  const resumen = resumirResultados([{ estado: "verificado", verificacion: ejemplo }, { estado: "pendiente" }, { estado: "error" }]);
  assert.equal(resumen.verificadas, 1); assert.equal(resumen.pendientes, 1); assert.equal(resumen.errores, 1);
  assert.equal(resumen.pagado, 22000); assert.equal(resumen.estimado, 20000); assert.equal(resumen.diferencia, 2000);
});
test("rechaza IDs ajenos, valoraciones desconocidas e importes ausentes", () => {
  assert.throws(() => normalizarVerificacion(ejemplo, 42));
  assert.throws(() => normalizarVerificacion({ ...ejemplo, totalReal: null }, 41));
  assert.throws(() => normalizarVerificacion({ ...ejemplo, valoracion: "inventado" }, 41));
});
