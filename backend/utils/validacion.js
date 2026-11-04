function esUrlImagenValida(valor) {
  if (typeof valor !== 'string' || valor.length > 2048) return false;
  if (/^\/uploads\/[A-Za-z0-9._-]+$/.test(valor)) return true;

  try {
    const url = new URL(valor);
    return (url.protocol === 'http:' || url.protocol === 'https:') && Boolean(url.hostname);
  } catch {
    return false;
  }
}

const HORARIOS = ['madrugador', 'nocturno', 'mixto'];
const FRECUENCIAS = ['nunca', 'ocasional', 'frecuente'];
const MAX_PRESUPUESTO = 2147483647; // límite de INTEGER en PostgreSQL

function presente(valor) {
  return valor !== undefined && valor !== null;
}

function enteroEntre(valor, minimo, maximo) {
  const numero = Number(valor);
  return valor !== '' && Number.isInteger(numero) && numero >= minimo && numero <= maximo;
}

/**
 * Valida las preferencias del perfil. Los campos ausentes se ignoran porque
 * la edición puede enviar solo algunos. Devuelve el mensaje de error o null.
 */
function validarPreferencias(datos) {
  if (presente(datos.presupuesto) && !enteroEntre(datos.presupuesto, 0, MAX_PRESUPUESTO)) {
    return 'El presupuesto debe ser un número entero positivo.';
  }
  if (presente(datos.horario) && !HORARIOS.includes(datos.horario)) {
    return 'El horario no es válido.';
  }
  if (presente(datos.limpieza) && !enteroEntre(datos.limpieza, 1, 5)) {
    return 'El nivel de limpieza debe estar entre 1 y 5.';
  }
  if (presente(datos.tolerancia_ruido) && !enteroEntre(datos.tolerancia_ruido, 1, 5)) {
    return 'La tolerancia al ruido debe estar entre 1 y 5.';
  }
  if (presente(datos.frecuencia_visitas) && !FRECUENCIAS.includes(datos.frecuencia_visitas)) {
    return 'La frecuencia de visitas no es válida.';
  }
  if (presente(datos.descripcion) && String(datos.descripcion).length > 1000) {
    return 'La descripción puede tener máximo 1000 caracteres.';
  }
  return null;
}

/** Valida los datos de una cuenta nueva. Devuelve el mensaje de error o null. */
function validarCuenta({ nombre, email, password }) {
  if (String(nombre).trim().length === 0 || String(nombre).length > 120) {
    return 'El nombre debe tener entre 1 y 120 caracteres.';
  }
  if (String(email).length > 160 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return 'El correo no es válido.';
  }
  if (String(password).length < 6) {
    return 'La contraseña debe tener al menos 6 caracteres.';
  }
  return null;
}

module.exports = { esUrlImagenValida, validarPreferencias, validarCuenta };
