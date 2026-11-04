const { test } = require('node:test');
const assert = require('node:assert');
const { validarCuenta, validarPreferencias } = require('../utils/validacion');
const { esBarrioValido } = require('../utils/barrios');

const cuenta = { nombre: 'Ana', email: 'ana@correo.co', password: 'clave123' };

test('acepta una cuenta válida', () => {
  assert.strictEqual(validarCuenta(cuenta), null);
});

test('rechaza correo inválido y contraseña corta', () => {
  assert.match(validarCuenta({ ...cuenta, email: 'ana' }), /correo/);
  assert.match(validarCuenta({ ...cuenta, password: '123' }), /contraseña/);
});

test('acepta valores de formulario como texto y campos ausentes', () => {
  assert.strictEqual(validarPreferencias({ presupuesto: '500000', limpieza: '3', horario: 'mixto' }), null);
  assert.strictEqual(validarPreferencias({}), null);
  assert.strictEqual(validarPreferencias({ presupuesto: null }), null);
});

test('rechaza preferencias fuera de rango', () => {
  assert.match(validarPreferencias({ presupuesto: -1 }), /presupuesto/);
  assert.match(validarPreferencias({ presupuesto: '' }), /presupuesto/);
  assert.match(validarPreferencias({ limpieza: 6 }), /limpieza/);
  assert.match(validarPreferencias({ tolerancia_ruido: 0 }), /ruido/);
  assert.match(validarPreferencias({ horario: 'tarde' }), /horario/);
  assert.match(validarPreferencias({ frecuencia_visitas: 'siempre' }), /visitas/);
  assert.match(validarPreferencias({ descripcion: 'x'.repeat(1001) }), /descripción/);
});

test('reconoce los barrios de la lista', () => {
  assert.ok(esBarrioValido('El Pilar'));
  assert.ok(esBarrioValido('Otro barrio de Pasto'));
  assert.ok(!esBarrioValido('Centro, Pasto'));
  assert.ok(!esBarrioValido(''));
});
