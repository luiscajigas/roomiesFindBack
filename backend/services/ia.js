/**
 * Cliente del proveedor de IA (Gemini) para RoomiesFind.
 *
 * La IA NUNCA decide quién es compatible: el Web Worker del frontend ya
 * calculó el score y las listas de coincidencias/conflictos con reglas
 * objetivas. Aquí solo se interpreta ese resultado en lenguaje natural.
 */

const MODELO_POR_DEFECTO = 'gemini-2.0-flash';
const TIMEOUT_MS = 20000;
const MAX_ITEMS_LISTA = 10;

function comoLista(items) {
  const limpios = (Array.isArray(items) ? items : [])
    .slice(0, MAX_ITEMS_LISTA)
    .map((item) => String(item).trim())
    .filter(Boolean);
  return limpios.length ? limpios.map((item) => `- ${item}`).join('\n') : '- (sin datos registrados)';
}

function construirPrompt({ nombreUsuario, nombreCandidato, score, coincidencias, conflictos }) {
  return [
    'Eres el asistente de RoomiesFind, una aplicación para encontrar compañeros de vivienda.',
    `Explica en español por qué ${nombreUsuario} y ${nombreCandidato} tienen un ${score}% de compatibilidad.`,
    'El puntaje ya fue calculado con reglas objetivas; tu único trabajo es interpretarlo, no modificarlo.',
    '',
    'Puntos en común:',
    comoLista(coincidencias),
    '',
    'Posibles temas para conversar antes de vivir juntos:',
    comoLista(conflictos),
    '',
    'Escribe entre 90 y 130 palabras en texto corrido, sin títulos, listas ni formato markdown.',
    'Usa un tono cercano y respetuoso. No inventes datos que no estén en las listas.',
    'Termina con un consejo práctico y concreto para la convivencia.'
  ].join('\n');
}

/**
 * Llama a Gemini y devuelve el texto de la explicación.
 * Lanza un Error con mensaje descriptivo si falta configuración o el
 * proveedor falla; nunca incluye la API key en los mensajes.
 */
async function generarExplicacion(contexto) {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    throw new Error('GEMINI_API_KEY no está configurada en el servidor.');
  }
  if (typeof fetch !== 'function') {
    throw new Error('Este servidor necesita Node.js 18 o superior (fetch no disponible).');
  }

  const modelo = process.env.GEMINI_MODEL || MODELO_POR_DEFECTO;
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(modelo)}:generateContent?key=${apiKey}`;
  const cuerpo = {
    contents: [{ role: 'user', parts: [{ text: construirPrompt(contexto) }] }],
    generationConfig: { temperature: 0.7, maxOutputTokens: 280 }
  };

  const controlador = new AbortController();
  const temporizador = setTimeout(() => controlador.abort(), TIMEOUT_MS);
  let respuesta;
  try {
    respuesta = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(cuerpo),
      signal: controlador.signal
    });
  } finally {
    clearTimeout(temporizador);
  }

  if (!respuesta.ok) {
    const detalle = await respuesta.text().catch(() => '');
    throw new Error(`El proveedor de IA respondió ${respuesta.status}: ${detalle.slice(0, 200)}`);
  }

  const datos = await respuesta.json();
  const texto = (datos?.candidates?.[0]?.content?.parts || [])
    .map((parte) => parte.text || '')
    .join('')
    .trim();

  if (!texto) {
    throw new Error('El proveedor de IA no devolvió texto.');
  }
  return texto;
}

module.exports = { generarExplicacion };
