const express = require('express');
const pool = require('../db');
const { requiereAuth } = require('../middleware/auth');

const router = express.Router();

/**
 * POST /api/compatibilidad/registrar
 * El frontend, después de calcular el score en el Web Worker, lo reporta
 * aquí para dejar un historial (analítica de qué tan compatibles resultan
 * los usuarios en promedio, cuáles perfiles se consultan más, etc.).
 * Body: { candidatoId, score }
 */
router.post('/registrar', requiereAuth, async (req, res) => {
  const { candidatoId, score } = req.body;
  if (!candidatoId || score === undefined) {
    return res.status(400).json({ error: 'candidatoId y score son obligatorios.' });
  }

  await pool.query(
    'INSERT INTO consultas_compatibilidad (usuario_id, candidato_id, score) VALUES ($1, $2, $3)',
    [req.usuarioId, candidatoId, score]
  );

  res.status(201).json({ registrado: true });
});

/**
 * POST /api/ia/explicar
 * Contrato ya definido para la fase de IA: recibe el score y el detalle de
 * puntos en común / posibles conflictos (ya calculados por reglas en el
 * frontend) y debe responder con una explicación en lenguaje natural.
 *
 * IMPORTANTE: la IA NO decide quién es compatible con quién -solo
 * interpreta un resultado que ya fue calculado objetivamente-, tal como se
 * definió en el diseño del proyecto. Por ahora responde un placeholder;
 * aquí es exactamente donde se conecta el proveedor de IA en la siguiente
 * fase (TODO marcado abajo).
 *
 * Body: { candidatoId, score, coincidencias: string[], conflictos: string[] }
 */
router.post('/explicar', requiereAuth, async (req, res) => {
  const { candidatoId, score, coincidencias = [], conflictos = [] } = req.body;

  if (!candidatoId || score === undefined) {
    return res.status(400).json({ error: 'candidatoId y score son obligatorios.' });
  }

  const registro = await pool.query(
    `INSERT INTO explicaciones_ia (usuario_id, candidato_id, score, explicacion, estado)
     VALUES ($1, $2, $3, $4, 'pendiente') RETURNING *`,
    [req.usuarioId, candidatoId, score, null]
  );

  // TODO(fase IA): reemplazar este bloque por la llamada real al proveedor
  // de IA, usando `score`, `coincidencias` y `conflictos` como contexto.
  // Al recibir la respuesta, hacer UPDATE de explicaciones_ia con
  // explicacion = <texto>, estado = 'generado'.
  const explicacionPlaceholder =
    'La explicación en lenguaje natural generada por IA se conectará en la ' +
    'siguiente fase del proyecto. Por ahora, revisa los puntos en común y ' +
    'posibles conflictos calculados directamente a partir de las preferencias.';

  res.json({
    id: registro.rows[0].id,
    estado: 'pendiente',
    explicacion: explicacionPlaceholder,
    score,
    coincidencias,
    conflictos
  });
});

module.exports = router;
