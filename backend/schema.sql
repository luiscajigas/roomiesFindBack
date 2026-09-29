-- Esquema de "Roomies" - Buscador inteligente de compañeros de vivienda
-- Compatible con PostgreSQL (Render, o cualquier Postgres administrado desde pgAdmin)

CREATE TABLE IF NOT EXISTS usuarios (
  id SERIAL PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS perfiles (
  usuario_id INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  presupuesto INTEGER NOT NULL,               -- presupuesto mensual (COP)
  zona VARCHAR(120) NOT NULL,                 -- zona/barrio donde busca vivienda
  horario VARCHAR(20) NOT NULL CHECK (horario IN ('madrugador','nocturno','mixto')),
  limpieza SMALLINT NOT NULL CHECK (limpieza BETWEEN 1 AND 5),   -- 1 = relajado, 5 = muy ordenado
  tolerancia_ruido SMALLINT NOT NULL CHECK (tolerancia_ruido BETWEEN 1 AND 5), -- 1 = silencio total, 5 = le da igual el ruido
  frecuencia_visitas VARCHAR(20) NOT NULL CHECK (frecuencia_visitas IN ('nunca','ocasional','frecuente')),
  tiene_mascotas BOOLEAN NOT NULL DEFAULT false,
  acepta_mascotas BOOLEAN NOT NULL DEFAULT true,
  descripcion TEXT,
  actualizado_en TIMESTAMP NOT NULL DEFAULT now()
);

-- Historial de comparaciones de compatibilidad calculadas (log/analítica)
CREATE TABLE IF NOT EXISTS consultas_compatibilidad (
  id SERIAL PRIMARY KEY,
  usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  candidato_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  score SMALLINT NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT now()
);

-- Explicaciones generadas (por IA en la fase siguiente); por ahora se
-- guardan como pendientes para dejar el contrato de datos ya definido.
CREATE TABLE IF NOT EXISTS explicaciones_ia (
  id SERIAL PRIMARY KEY,
  usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  candidato_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  score SMALLINT NOT NULL,
  explicacion TEXT,
  estado VARCHAR(20) NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente','generado')),
  creado_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_consultas_usuario ON consultas_compatibilidad(usuario_id);
CREATE INDEX IF NOT EXISTS idx_explicaciones_par ON explicaciones_ia(usuario_id, candidato_id);
