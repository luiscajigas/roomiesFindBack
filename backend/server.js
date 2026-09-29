require('dotenv').config();
const express = require('express');
const cors = require('cors');

const authRoutes = require('./routes/auth');
const perfilesRoutes = require('./routes/perfiles');
const compatibilidadRoutes = require('./routes/compatibilidad');

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

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

app.listen(PORT, () => {
  console.log(`Backend de Roomies corriendo en http://localhost:${PORT}`);
});
