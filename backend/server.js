require('dotenv').config();
const fs = require('fs/promises');
const path = require('path');
const express = require('express');
const cors = require('cors');
const pool = require('./db');
const { uploadsDir, prepararDirectorioFotos } = require('./utils/upload');

const authRoutes = require('./routes/auth');
const perfilesRoutes = require('./routes/perfiles');
const compatibilidadRoutes = require('./routes/compatibilidad');

const app = express();
const PORT = process.env.PORT || 3001;
const origenesPermitidos = (process.env.FRONTEND_URL || (process.env.NODE_ENV === 'production' ? '' : 'http://localhost:4300'))
  .split(',')
  .map((origen) => origen.trim())
  .filter(Boolean);

// Además de los orígenes exactos configurados, se aceptan las URLs de
// preview que Vercel genera para el mismo proyecto (<proyecto>-*.vercel.app).
const proyectoVercel = origenesPermitidos
  .map((origen) => {
    try {
      return new URL(origen).hostname.match(/^([a-z0-9-]+)\.vercel\.app$/i)?.[1] || null;
    } catch {
      return null;
    }
  })
  .find(Boolean);

function origenPermitido(origen) {
  if (!origen) return true; // peticiones sin Origin (curl, comprobaciones de salud)
  if (origenesPermitidos.includes(origen)) return true;
  if (!proyectoVercel) return false;
  try {
    const { hostname } = new URL(origen);
    return hostname.endsWith('.vercel.app') && hostname.startsWith(`${proyectoVercel}-`);
  } catch {
    return false;
  }
}

app.use(cors({
  origin: (origen, callback) => callback(null, origenPermitido(origen))
}));
app.use(express.json());
app.use('/uploads', express.static(uploadsDir, { index: false }));

app.get('/api/salud', (req, res) => {
  res.json({ estado: 'ok', servicio: 'roomies-backend' });
});

app.use('/api/auth', authRoutes);
app.use('/api/perfiles', perfilesRoutes);
app.use('/api/compatibilidad', compatibilidadRoutes);
app.use('/api/ia', compatibilidadRoutes); // /api/ia/explicar vive en el mismo router

app.use((req, res) => {
  res.status(404).json({ error: 'Ruta no encontrada.' });
});

// Responde en JSON cualquier error no controlado de las rutas.
app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  if (err.status >= 400 && err.status < 500) {
    return res.status(err.status).json({ error: 'La solicitud no es válida.' });
  }
  console.error(err);
  res.status(500).json({ error: 'Error interno del servidor.' });
});

async function iniciarServidor() {
  await prepararDirectorioFotos();
  // Crea las tablas si no existen; es seguro ejecutarlo en cada arranque.
  const esquema = await fs.readFile(path.join(__dirname, 'schema.sql'), 'utf8');
  await pool.query(esquema);
  app.listen(PORT, () => {
    console.log(`Backend de Roomies corriendo en http://localhost:${PORT}`);
  });
}

iniciarServidor().catch((error) => {
  console.error('No se pudo preparar la base de datos o el almacenamiento de fotos:', error);
  pool.end().finally(() => process.exit(1));
});
