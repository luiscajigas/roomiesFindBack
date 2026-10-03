const path = require('node:path');
const fs = require('node:fs');
const multer = require('multer');

const uploadsDir = process.env.UPLOADS_DIR
  ? path.resolve(process.env.UPLOADS_DIR)
  : path.resolve(__dirname, '..', 'uploads');
const tiposPermitidos = new Set(['image/jpeg', 'image/png', 'image/webp', 'image/gif']);

const subirFoto = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 5 * 1024 * 1024, files: 1 },
  fileFilter: (req, file, callback) => {
    if (!tiposPermitidos.has(file.mimetype)) {
      const error = new Error('Elige una imagen JPG, PNG, WEBP o GIF.');
      error.status = 400;
      callback(error);
      return;
    }
    callback(null, true);
  }
});

function recibirFoto(req, res, next) {
  subirFoto.single('foto')(req, res, (error) => {
    if (!error) {
      next();
      return;
    }

    if (error instanceof multer.MulterError && error.code === 'LIMIT_FILE_SIZE') {
      res.status(400).json({ error: 'La foto debe pesar 5 MB o menos.' });
      return;
    }
    if (error instanceof multer.MulterError && error.code === 'LIMIT_UNEXPECTED_FILE') {
      res.status(400).json({ error: 'Solo puedes subir una foto de perfil.' });
      return;
    }
    res.status(error.status || 400).json({ error: error.message || 'No se pudo recibir la foto.' });
  });
}

async function prepararDirectorioFotos() {
  await fs.promises.mkdir(uploadsDir, { recursive: true });
}

async function esImagenReal(file) {
  const buffer = file.buffer;
  if (!Buffer.isBuffer(buffer)) return false;

  switch (file.mimetype) {
    case 'image/jpeg':
      return buffer.length >= 3 && buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff;
    case 'image/png':
      return buffer.length >= 8 && buffer.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]));
    case 'image/webp':
      return buffer.length >= 12 && buffer.toString('ascii', 0, 4) === 'RIFF' && buffer.toString('ascii', 8, 12) === 'WEBP';
    case 'image/gif':
      return buffer.length >= 6 && ['GIF87a', 'GIF89a'].includes(buffer.toString('ascii', 0, 6));
    default:
      return false;
  }
}

module.exports = { uploadsDir, recibirFoto, prepararDirectorioFotos, esImagenReal };
