const express = require('express');
const pool = require('../db');
const { requiereAuth } = require('../middleware/auth');
const { generarExplicacion } = require('../services/ia');

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
    `INSERT INTO recomendaciones (usuario_id, candidato_id, score)
     VALUES ($1, $2, $3)
     ON CONFLICT (usuario_id, candidato_id)
     DO UPDATE SET score = EXCLUDED.score, actualizado_en = now()`,
    [req.usuarioId, candidatoId, score]
  );

  res.status(201).json({ registrado: true });
});

/**
 * POST /api/ia/explicar
 * Recibe el score y el detalle de puntos en común / posibles conflictos (ya
 * calculados por reglas en el frontend) y responde con una explicación en
 * lenguaje natural generada por IA.
 *
 * IMPORTANTE: la IA NO decide quién es compatible con quién -solo
 * interpreta un resultado que ya fue calculado objetivamente-, tal como se
 * definió en el diseño del proyecto.
 *
 * Para minimizar llamadas al proveedor, si ya existe una explicación
 * generada para la misma pareja con el mismo score, se devuelve la guardada
 * (desdeCache: true) sin volver a llamar a la IA.
 *
 * Body: { candidatoId, score, coincidencias: string[], conflictos: string[] }
 */
router.post('/explicar', requiereAuth, async (req, res) => {
  const { candidatoId, score, coincidencias = [], conflictos = [] } = req.body;
  const idCandidato = Number(candidatoId);
  const puntaje = Number(score);

  if (!Number.isSafeInteger(idCandidato) || idCandidato <= 0
    || !Number.isFinite(puntaje) || puntaje < 0 || puntaje > 100) {
    return res.status(400).json({ error: 'candidatoId y score son obligatorios y deben ser válidos.' });
  }
  if (idCandidato === req.usuarioId) {
    return res.status(400).json({ error: 'No puedes pedir una explicación de compatibilidad contigo mismo.' });
  }

  try {
    const nombres = await pool.query(
      'SELECT id, nombre FROM usuarios WHERE id = ANY($1::int[])',
      [[req.usuarioId, idCandidato]]
    );
    const candidato = nombres.rows.find((u) => u.id === idCandidato);
    if (!candidato) {
      return res.status(404).json({ error: 'El perfil del candidato no existe.' });
    }
    const usuario = nombres.rows.find((u) => u.id === req.usuarioId);

    const guardada = await pool.query(
      `SELECT id, explicacion FROM explicaciones_ia
       WHERE usuario_id = $1 AND candidato_id = $2 AND score = $3
         AND estado = 'generado' AND explicacion IS NOT NULL
       ORDER BY creado_en DESC LIMIT 1`,
      [req.usuarioId, idCandidato, puntaje]
    );
    if (guardada.rows[0]) {
      return res.json({
        id: guardada.rows[0].id,
        estado: 'generado',
        explicacion: guardada.rows[0].explicacion,
        score: puntaje,
        coincidencias,
        conflictos,
        desdeCache: true
      });
    }

    const registro = await pool.query(
      `INSERT INTO explicaciones_ia (usuario_id, candidato_id, score, explicacion, estado)
       VALUES ($1, $2, $3, NULL, 'pendiente') RETURNING id`,
      [req.usuarioId, idCandidato, puntaje]
    );
    const explicacionId = registro.rows[0].id;

    const texto = await generarExplicacion({
      nombreUsuario: usuario?.nombre || 'la persona usuaria',
      nombreCandidato: candidato.nombre,
      score: puntaje,
      coincidencias,
      conflictos
    });

    await pool.query(
      `UPDATE explicaciones_ia SET explicacion = $1, estado = 'generado' WHERE id = $2`,
      [texto, explicacionId]
    );

    res.json({
      id: explicacionId,
      estado: 'generado',
      explicacion: texto,
      score: puntaje,
      coincidencias,
      conflictos,
      desdeCache: false
    });
  } catch (error) {
    console.error('No se pudo generar la explicación con IA:', error.message);
    res.status(503).json({
      error: 'La explicación con IA no está disponible en este momento. Inténtalo de nuevo en unos segundos.'
    });
  }
});

module.exports = router;
