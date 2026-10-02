const express = require('express');
const pool = require('../db');
const { requiereAuth } = require('../middleware/auth');
const { esUrlImagenValida } = require('../utils/validacion');

const router = express.Router();

/**
 * GET /api/perfiles/me
 * Perfil del usuario autenticado.
 */
router.get('/me', requiereAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT u.id, u.nombre, u.email, p.*, z.nombre AS zona
     FROM usuarios u
     JOIN perfiles p ON p.usuario_id = u.id
     JOIN zonas z ON z.id = p.zona_id
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
    frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion, foto_url
  } = req.body;
  if (foto_url && !esUrlImagenValida(foto_url)) {
    return res.status(400).json({ error: 'El enlace de la foto debe ser una URL válida que use HTTP o HTTPS.' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    let zonaId = null;
    if (zona !== undefined) {
      const zonaResult = await client.query(
        `INSERT INTO zonas (nombre) VALUES ($1)
         ON CONFLICT (nombre) DO UPDATE SET nombre = EXCLUDED.nombre
         RETURNING id`,
        [zona]
      );
      zonaId = zonaResult.rows[0].id;
    }

    await client.query(
      `UPDATE perfiles SET
       presupuesto = COALESCE($1, presupuesto),
       zona_id = COALESCE($2, zona_id),
       horario = COALESCE($3, horario),
       limpieza = COALESCE($4, limpieza),
       tolerancia_ruido = COALESCE($5, tolerancia_ruido),
       frecuencia_visitas = COALESCE($6, frecuencia_visitas),
       tiene_mascotas = COALESCE($7, tiene_mascotas),
       acepta_mascotas = COALESCE($8, acepta_mascotas),
       descripcion = COALESCE($9, descripcion),
       foto_url = COALESCE($10, foto_url),
       actualizado_en = now()
       WHERE usuario_id = $11`,
      [presupuesto, zonaId, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion, foto_url, req.usuarioId]
    );
    await client.query('COMMIT');

    const result = await client.query(
      `SELECT p.*, z.nombre AS zona FROM perfiles p
       JOIN zonas z ON z.id = p.zona_id WHERE p.usuario_id = $1`,
      [req.usuarioId]
    );
    res.json(result.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error(err);
    res.status(500).json({ error: 'No se pudo actualizar el perfil.' });
  } finally {
    client.release();
  }
});

/**
 * GET /api/perfiles/candidatos
 * Devuelve todos los perfiles EXCEPTO el del usuario autenticado.
 * El cálculo de compatibilidad lo hace el frontend en un Web Worker
 * (para no bloquear la interfaz cuando hay muchos perfiles), no el backend.
 */
router.get('/candidatos', requiereAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT u.id, u.nombre, p.*, z.nombre AS zona
     FROM usuarios u
     JOIN perfiles p ON p.usuario_id = u.id
     JOIN zonas z ON z.id = p.zona_id
     WHERE u.id != $1
     ORDER BY u.id`,
    [req.usuarioId]
  );
  res.json(result.rows);
});

module.exports = router;
