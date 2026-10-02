require('dotenv').config();
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

app.use(cors({ origin: origenesPermitidos.length ? origenesPermitidos : false }));
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

async function iniciarServidor() {
  await prepararDirectorioFotos();
  await pool.query(`
    ALTER TABLE perfiles ADD COLUMN IF NOT EXISTS foto_url TEXT;
    ALTER TABLE perfiles DROP CONSTRAINT IF EXISTS perfiles_foto_url_check;
    ALTER TABLE perfiles ADD CONSTRAINT perfiles_foto_url_check CHECK (
      foto_url IS NULL OR
      foto_url ~* '^https?://' OR
      foto_url ~ '^/uploads/[A-Za-z0-9._-]+$'
    );
  `);
  app.listen(PORT, () => {
    console.log(`Backend de Roomies corriendo en http://localhost:${PORT}`);
  });
}

iniciarServidor().catch((error) => {
  console.error('No se pudo preparar la base de datos o el almacenamiento de fotos:', error);
  pool.end().finally(() => process.exit(1));
});
