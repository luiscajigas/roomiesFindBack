const jwt = require('jsonwebtoken');

const SECRETO = process.env.JWT_SECRET || (process.env.NODE_ENV === 'production' ? null : 'secreto-local-de-desarrollo');

if (!SECRETO) {
  throw new Error('JWT_SECRET es obligatorio en producción.');
}

function requiereAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;

  if (!token) {
    return res.status(401).json({ error: 'No autenticado. Falta el token.' });
  }

  try {
    const payload = jwt.verify(token, SECRETO);
    req.usuarioId = payload.usuarioId;
    next();
  } catch (err) {
    return res.status(401).json({ error: 'Token inválido o expirado.' });
  }
}

module.exports = { requiereAuth, SECRETO };
