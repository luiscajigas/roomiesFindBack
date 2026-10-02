-- Esquema de "Roomies" - Buscador inteligente de compañeros de vivienda
-- Compatible con PostgreSQL (Render, o cualquier Postgres administrado desde pgAdmin)

CREATE TABLE IF NOT EXISTS usuarios (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  creado_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS zonas (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre VARCHAR(160) NOT NULL UNIQUE,
  creada_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS perfiles (
  usuario_id INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  presupuesto INTEGER NOT NULL CHECK (presupuesto >= 0),
  zona_id INTEGER NOT NULL REFERENCES zonas(id),
  horario VARCHAR(20) NOT NULL CHECK (horario IN ('madrugador','nocturno','mixto')),
  limpieza SMALLINT NOT NULL CHECK (limpieza BETWEEN 1 AND 5),   -- 1 = relajado, 5 = muy ordenado
  tolerancia_ruido SMALLINT NOT NULL CHECK (tolerancia_ruido BETWEEN 1 AND 5), -- 1 = silencio total, 5 = le da igual el ruido
  frecuencia_visitas VARCHAR(20) NOT NULL CHECK (frecuencia_visitas IN ('nunca','ocasional','frecuente')),
  tiene_mascotas BOOLEAN NOT NULL DEFAULT false,
  acepta_mascotas BOOLEAN NOT NULL DEFAULT true,
  descripcion TEXT,
  foto_url TEXT CHECK (
    foto_url IS NULL OR
    foto_url ~* '^https?://' OR
    foto_url ~ '^/uploads/[A-Za-z0-9._-]+$'
  ), -- Ruta relativa del archivo de perfil guardado por el backend
  actualizado_en TIMESTAMP NOT NULL DEFAULT now()
);

ALTER TABLE perfiles
  ADD COLUMN IF NOT EXISTS foto_url TEXT;

ALTER TABLE perfiles
  DROP CONSTRAINT IF EXISTS perfiles_foto_url_check;

ALTER TABLE perfiles
  ADD CONSTRAINT perfiles_foto_url_check CHECK (
    foto_url IS NULL OR
    foto_url ~* '^https?://' OR
    foto_url ~ '^/uploads/[A-Za-z0-9._-]+$'
  );

CREATE TABLE IF NOT EXISTS recomendaciones (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  candidato_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  score SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
  actualizado_en TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (usuario_id, candidato_id),
  CHECK (usuario_id <> candidato_id)
);

-- Explicaciones generadas (por IA en la fase siguiente); por ahora se
-- guardan como pendientes para dejar el contrato de datos ya definido.
CREATE TABLE IF NOT EXISTS explicaciones_ia (
  id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  candidato_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  score SMALLINT NOT NULL CHECK (score BETWEEN 0 AND 100),
  explicacion TEXT,
  estado VARCHAR(20) NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente','generado')),
  creado_en TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_perfiles_zona ON perfiles(zona_id);
CREATE INDEX IF NOT EXISTS idx_recomendaciones_usuario ON recomendaciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_recomendaciones_candidato ON recomendaciones(candidato_id);
CREATE INDEX IF NOT EXISTS idx_explicaciones_par ON explicaciones_ia(usuario_id, candidato_id);
