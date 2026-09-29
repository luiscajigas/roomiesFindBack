const express = require('express');
const pool = require('../db');
const { requiereAuth } = require('../middleware/auth');

const router = express.Router();

/**
 * GET /api/perfiles/me
 * Perfil del usuario autenticado.
 */
router.get('/me', requiereAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT u.id, u.nombre, u.email, p.*
     FROM usuarios u JOIN perfiles p ON p.usuario_id = u.id
     WHERE u.id = $1`,
    [req.usuarioId]
  );
  if (!result.rows[0]) return res.status(404).json({ error: 'Perfil no encontrado.' });
  res.json(result.rows[0]);
});

/**
 * PUT /api/perfiles/me
 * Actualiza el perfil del usuario autenticado.
 */
router.put('/me', requiereAuth, async (req, res) => {
  const {
    presupuesto, zona, horario, limpieza, tolerancia_ruido,
    frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion
  } = req.body;

  const result = await pool.query(
    `UPDATE perfiles SET
       presupuesto = COALESCE($1, presupuesto),
       zona = COALESCE($2, zona),
       horario = COALESCE($3, horario),
       limpieza = COALESCE($4, limpieza),
       tolerancia_ruido = COALESCE($5, tolerancia_ruido),
       frecuencia_visitas = COALESCE($6, frecuencia_visitas),
       tiene_mascotas = COALESCE($7, tiene_mascotas),
       acepta_mascotas = COALESCE($8, acepta_mascotas),
       descripcion = COALESCE($9, descripcion),
       actualizado_en = now()
     WHERE usuario_id = $10
     RETURNING *`,
    [presupuesto, zona, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion, req.usuarioId]
  );

  res.json(result.rows[0]);
});

/**
 * GET /api/perfiles/candidatos
 * Devuelve todos los perfiles EXCEPTO el del usuario autenticado.
 * El cálculo de compatibilidad lo hace el frontend en un Web Worker
 * (para no bloquear la interfaz cuando hay muchos perfiles), no el backend.
 */
router.get('/candidatos', requiereAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT u.id, u.nombre, p.*
     FROM usuarios u JOIN perfiles p ON p.usuario_id = u.id
     WHERE u.id != $1
     ORDER BY u.id`,
    [req.usuarioId]
  );
  res.json(result.rows);
});

module.exports = router;
