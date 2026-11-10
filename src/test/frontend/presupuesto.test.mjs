import assert from "node:assert/strict";
import test from "node:test";
import {
  ultimosPrecios,
  calcularPresupuesto,
} from "../../main/resources/static/assets/js/presupuesto.js";

const productos = [{ id: 1, nombre: "Vacío" }];
const locales = [
  { id: 1, nombre: "Mismo nombre", direccion: "A" },
  { id: 2, nombre: "Mismo nombre", direccion: "B" },
];
const fila = (id, localId, precio, fecha) => ({
  id, local: { id: localId }, producto: { id: 1 }, precio, fecha,
});
const precios = [
  fila(10, 1, 11000, "2026-11-09"),
  fila(2, 1, 12000, "2026-11-10"),
  fila(3, 1, 13000, "2026-11-10"),
  fila(4, 2, 9000, "2026-11-09"),
];

test("elige por fecha e ID sin mezclar locales con el mismo nombre", () => {
  const resultado = ultimosPrecios(precios, productos, locales);
  assert.equal(resultado.length, 2);
  assert.equal(resultado[0].productos[0].precio, 13000);
  assert.equal(resultado[1].productos[0].precio, 9000);
  assert.equal(ultimosPrecios(precios, productos, []).length, 0);
  assert.equal(ultimosPrecios(precios, [], locales).length, 0);
});

test("redondea cada subtotal a centavos y admite medio kilo", () => {
  const publicados = { productos: [{ id: 1, nombre: "Vacío", precio: 10000.01, fecha: "2026-11-09" }] };
  const presupuesto = calcularPresupuesto(locales[0], publicados, [{ productoId: 1, cantidad: 1.5 }]);
  assert.equal(presupuesto.total, 15000.02);
  assert.equal(presupuesto.items[0].subtotal, 15000.02);
});

test("rechaza cantidades inválidas y listas incompletas", () => {
  const publicados = ultimosPrecios(precios, productos, locales)[0];
  for (const cantidad of [0, -1, 0.25, 1.2, NaN, 1000.5])
    assert.throws(() => calcularPresupuesto(locales[0], publicados, [{ productoId: 1, cantidad }]));
  assert.throws(() => calcularPresupuesto(locales[0], null, [{ productoId: 1, cantidad: 1 }]));
  assert.throws(() => calcularPresupuesto(locales[0], publicados, []));
});
