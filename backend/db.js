const { Pool } = require('pg');

// DATABASE_URL funciona tanto para Postgres local como para el Postgres
// gestionado en Render (o cualquier proveedor). En Render se necesita SSL.
const usarSSL = process.env.DATABASE_URL?.includes('render.com');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: usarSSL ? { rejectUnauthorized: false } : false
});

module.exports = pool;
