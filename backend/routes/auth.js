const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db');
const { SECRETO } = require('../middleware/auth');
const { recibirFoto, eliminarFoto, esImagenReal } = require('../utils/upload');

const router = express.Router();

function firmarToken(usuarioId) {
  return jwt.sign({ usuarioId }, SECRETO, { expiresIn: '7d' });
}

/**
 * POST /api/auth/registro
 * Crea el usuario y su perfil en una sola petición (más simple para el
 * flujo de onboarding: nunca hay un usuario sin perfil).
 */
router.post('/registro', recibirFoto, async (req, res) => {
  const {
    nombre, email, password,
    presupuesto, zona, horario, limpieza, tolerancia_ruido,
    frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion
  } = req.body;

  if (!nombre || !email || !password || !presupuesto || !zona || !horario) {
    await eliminarFoto(req.file?.path).catch((error) => console.error('No se pudo limpiar la foto temporal:', error));
    return res.status(400).json({ error: 'Faltan campos obligatorios del registro o del perfil.' });
  }
  if (req.file) {
    try {
      if (!(await esImagenReal(req.file))) {
        await eliminarFoto(req.file.path);
        return res.status(400).json({ error: 'El archivo seleccionado no es una imagen válida.' });
      }
    } catch (error) {
      await eliminarFoto(req.file.path).catch((cleanupError) => console.error('No se pudo limpiar la foto temporal:', cleanupError));
      console.error('No se pudo verificar la foto recibida:', error);
      return res.status(500).json({ error: 'No se pudo verificar la foto seleccionada.' });
    }
  }

  let client;
  try {
    client = await pool.connect();
  } catch (error) {
    await eliminarFoto(req.file?.path).catch((cleanupError) => console.error('No se pudo limpiar la foto temporal:', cleanupError));
    console.error('No se pudo conectar con la base de datos durante el registro:', error);
    return res.status(500).json({ error: 'No se pudo completar el registro.' });
  }
  try {
    await client.query('BEGIN');

    const passwordHash = await bcrypt.hash(password, 10);
    const usuarioResult = await client.query(
      'INSERT INTO usuarios (nombre, email, password_hash) VALUES ($1, $2, $3) RETURNING id, nombre, email',
      [nombre, email, passwordHash]
    );
    const usuario = usuarioResult.rows[0];
    const zonaResult = await client.query(
      `INSERT INTO zonas (nombre) VALUES ($1)
       ON CONFLICT (nombre) DO UPDATE SET nombre = EXCLUDED.nombre
       RETURNING id`,
      [zona]
    );

    const fotoUrl = req.file ? `/uploads/${req.file.filename}` : null;
    await client.query(
      `INSERT INTO perfiles
        (usuario_id, presupuesto, zona_id, horario, limpieza, tolerancia_ruido, frecuencia_visitas, tiene_mascotas, acepta_mascotas, descripcion, foto_url)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
      [
        usuario.id, presupuesto, zonaResult.rows[0].id, horario, limpieza ?? 3, tolerancia_ruido ?? 3,
        frecuencia_visitas ?? 'ocasional', tiene_mascotas === 'true',
        acepta_mascotas === undefined ? true : acepta_mascotas === 'true',
        descripcion || null, fotoUrl
      ]
    );

    await client.query('COMMIT');

    const token = firmarToken(usuario.id);
    res.status(201).json({ token, usuario });
  } catch (err) {
    await client.query('ROLLBACK');
    await eliminarFoto(req.file?.path).catch((error) => console.error('No se pudo limpiar la foto temporal:', error));
    if (err.code === '23505') {
      return res.status(409).json({ error: 'Ya existe una cuenta con ese correo.' });
    }
    console.error(err);
    res.status(500).json({ error: 'Error al registrar el usuario.' });
  } finally {
    client.release();
  }
});

/**
 * POST /api/auth/login
 */
router.post('/login', async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res.status(400).json({ error: 'email y password son obligatorios.' });
  }

  const result = await pool.query('SELECT * FROM usuarios WHERE email = $1', [email]);
  const usuario = result.rows[0];

  if (!usuario) {
    return res.status(401).json({ error: 'Credenciales inválidas.' });
  }

  const coincide = await bcrypt.compare(password, usuario.password_hash);
  if (!coincide) {
    return res.status(401).json({ error: 'Credenciales inválidas.' });
  }

  const token = firmarToken(usuario.id);
  res.json({
    token,
    usuario: { id: usuario.id, nombre: usuario.nombre, email: usuario.email }
  });
});

module.exports = router;
