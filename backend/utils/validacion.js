function esUrlImagenValida(valor) {
  if (typeof valor !== 'string' || valor.length > 2048) return false;

  try {
    const url = new URL(valor);
    return (url.protocol === 'http:' || url.protocol === 'https:') && Boolean(url.hostname);
  } catch {
    return false;
  }
}

module.exports = { esUrlImagenValida };
