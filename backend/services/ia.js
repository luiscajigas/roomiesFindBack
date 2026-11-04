/**
 * Cliente de IA para RoomiesFind: usa Claude y, si falla o no está
 * configurado, Gemini como respaldo.
 *
 * La IA NUNCA decide quién es compatible: el Web Worker del frontend ya
 * calculó el score y las listas de coincidencias/conflictos con reglas
 * objetivas. Aquí solo se interpreta ese resultado en lenguaje natural.
 */

const { Anthropic } = require('@anthropic-ai/sdk');

const MODELO_CLAUDE_POR_DEFECTO = 'claude-opus-5-5';
const MODELOS_POR_DEFECTO = ['gemini-3.8-flash', 'gemini-2.5-flash'];
const TIMEOUT_MS = 20000;
const TIMEOUT_CLAUDE_MS = 30000;
const MAX_INTENTOS_POR_MODELO = 3;
const ESPERA_BASE_MS = 1200;
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
    `Explica en español, dirigida a ${nombreUsuario}, por qué ${nombreUsuario} y ${nombreCandidato} tienen un ${score}% de compatibilidad.`,
    'El puntaje ya fue calculado con reglas objetivas; tu único trabajo es interpretarlo, no modificarlo.',
    '',
    'Puntos en común:',
    comoLista(coincidencias),
    '',
    'Posibles temas para conversar antes de vivir juntos:',
    comoLista(conflictos),
    '',
    'Empieza directamente con la explicación: no escribas el nombre de la aplicación, encabezados ni saludos.',
    'Estructura: primero lo que comparten y por qué facilita la convivencia, luego lo que conviene acordar, y termina con un consejo práctico y concreto.',
    'Escribe entre 90 y 130 palabras en texto corrido, sin títulos, listas ni formato markdown.',
    'Usa un tono cercano y respetuoso. No inventes datos que no estén en las listas.',
    'La respuesta debe terminar con una oración completa.'
  ].join('\n');
}

/**
 * Si el proveedor cortó el texto a mitad de una oración, devuelve el texto
 * hasta la última oración completa para no mostrar frases incompletas.
 */
function hastaUltimaOracionCompleta(texto) {
  if (/[.!?…]["'”)]?\s*$/.test(texto)) return texto;
  const corte = Math.max(texto.lastIndexOf('.'), texto.lastIndexOf('!'), texto.lastIndexOf('?'));
  return corte > 40 ? texto.slice(0, corte + 1).trim() : texto;
}

/**
 * Genera la explicación con Claude. El SDK ya reintenta solo ante 429, 5xx y
 * errores de conexión. Si los filtros de seguridad rechazan la petición, el
 * parámetro fallbacks la pasa a otro modelo (Haiku no lo admite).
 */
async function generarConClaude(contexto) {
  const modelo = process.env.ANTHROPIC_MODEL || MODELO_CLAUDE_POR_DEFECTO;
  const respaldo = modelo.startsWith('claude-haiku')
    ? {}
    : { betas: ['server-side-fallback-2026-07-01'], fallbacks: 'default' };

  const cliente = new Anthropic({ timeout: TIMEOUT_CLAUDE_MS });
  const respuesta = await cliente.beta.messages.create({
    model: modelo,
    max_tokens: 4000,
    output_config: { effort: 'low' },
    messages: [{ role: 'user', content: construirPrompt(contexto) }],
    ...respaldo
  });

  if (respuesta.stop_reason === 'refusal') {
    throw new Error('Claude rechazó generar la explicación.');
  }
  const texto = respuesta.content
    .filter((bloque) => bloque.type === 'text')
    .map((bloque) => bloque.text)
    .join('')
    .trim();
  if (!texto) {
    throw new Error('Claude no devolvió texto.');
  }
  return hastaUltimaOracionCompleta(texto);
}

/**
 * Llama a Gemini y devuelve el texto de la explicación.
 * Lanza un Error con mensaje descriptivo si falta configuración o el
 * proveedor falla; nunca incluye la API key en los mensajes.
 */
async function llamarGemini(modelo, apiKey, contexto) {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(modelo)}:generateContent?key=${apiKey}`;
  const cuerpo = {
    contents: [{ role: 'user', parts: [{ text: construirPrompt(contexto) }] }],
    generationConfig: { temperature: 0.7, maxOutputTokens: 1024 }
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
    const error = new Error(`El proveedor de IA respondió ${respuesta.status}: ${detalle.slice(0, 200)}`);
    error.status = respuesta.status;
    throw error;
  }

  const datos = await respuesta.json();
  const texto = (datos?.candidates?.[0]?.content?.parts || [])
    .map((parte) => parte.text || '')
    .join('')
    .trim();

  if (!texto) {
    throw new Error('El proveedor de IA no devolvió texto.');
  }
  return hastaUltimaOracionCompleta(texto);
}

function esperar(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * Genera la explicación con Gemini, con reintentos ante errores transitorios
 * (429 cuota, 503 alta demanda, timeout) y con modelo de respaldo si el
 * configurado ya no existe (404).
 */
async function generarConGemini(contexto) {
  const apiKey = process.env.GEMINI_API_KEY;
  if (typeof fetch !== 'function') {
    throw new Error('Este servidor necesita Node.js 18 o superior (fetch no disponible).');
  }

  const modelos = process.env.GEMINI_MODEL
    ? [process.env.GEMINI_MODEL, ...MODELOS_POR_DEFECTO.filter((m) => m !== process.env.GEMINI_MODEL)]
    : MODELOS_POR_DEFECTO;

  let ultimoError = null;
  for (const modelo of modelos) {
    for (let intento = 1; intento <= MAX_INTENTOS_POR_MODELO; intento++) {
      try {
        return await llamarGemini(modelo, apiKey, contexto);
      } catch (error) {
        ultimoError = error;
        if (error.status === 404) break; // modelo retirado: probar el siguiente
        const transitorio = error.status === 429 || error.status === 503 || error.name === 'AbortError';
        if (!transitorio || intento === MAX_INTENTOS_POR_MODELO) break;
        await esperar(ESPERA_BASE_MS * intento);
      }
    }
  }
  throw ultimoError || new Error('No se pudo contactar con el proveedor de IA.');
}

/**
 * Intenta con cada proveedor configurado, en orden: Claude y luego Gemini.
 */
async function generarExplicacion(contexto) {
  const proveedores = [];
  if (process.env.ANTHROPIC_API_KEY) proveedores.push(['Claude', generarConClaude]);
  if (process.env.GEMINI_API_KEY) proveedores.push(['Gemini', generarConGemini]);
  if (!proveedores.length) {
    throw new Error('Configura ANTHROPIC_API_KEY o GEMINI_API_KEY en el servidor.');
  }

  let ultimoError;
  for (const [nombre, generar] of proveedores) {
    try {
      return await generar(contexto);
    } catch (error) {
      ultimoError = error;
      console.warn(`Falló la IA con ${nombre}: ${error.message}`);
    }
  }
  throw ultimoError;
}

module.exports = { generarExplicacion };
