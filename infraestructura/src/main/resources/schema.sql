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
    foto_url ~ '^/uploads/[A-Za-z0-9._-]+$' OR
    foto_url ~ '^/api/perfiles/[0-9]+/foto$'
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
    foto_url ~ '^/uploads/[A-Za-z0-9._-]+$' OR
    foto_url ~ '^/api/perfiles/[0-9]+/foto$'
  );

CREATE TABLE IF NOT EXISTS fotos_perfil (
  usuario_id INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  tipo_mime VARCHAR(32) NOT NULL CHECK (tipo_mime IN ('image/jpeg', 'image/png', 'image/webp', 'image/gif')),
  contenido BYTEA NOT NULL,
  actualizado_en TIMESTAMP NOT NULL DEFAULT now()
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

ALTER TABLE explicaciones_ia
  ADD COLUMN IF NOT EXISTS idioma VARCHAR(2) NOT NULL DEFAULT 'es';

CREATE TABLE IF NOT EXISTS traducciones (
  huella CHAR(64) NOT NULL,
  idioma VARCHAR(2) NOT NULL,
  traduccion TEXT NOT NULL,
  creada_en TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY (huella, idioma)
);

-- Chat: cada pareja tiene una sola conversación, con el id menor primero.
CREATE TABLE IF NOT EXISTS conversaciones (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  usuario_a INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  usuario_b INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  creada_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (usuario_a, usuario_b),
  CHECK (usuario_a < usuario_b)
);

CREATE TABLE IF NOT EXISTS mensajes (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conversacion_id BIGINT NOT NULL REFERENCES conversaciones(id) ON DELETE CASCADE,
  remitente_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  contenido TEXT NOT NULL CHECK (char_length(contenido) BETWEEN 1 AND 2000),
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  leido_en TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conversaciones_b ON conversaciones(usuario_b);
CREATE INDEX IF NOT EXISTS idx_mensajes_conversacion ON mensajes(conversacion_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_mensajes_sin_leer ON mensajes(conversacion_id, remitente_id) WHERE leido_en IS NULL;

-- Moderación: quien bloquea deja de ver a la otra persona y ella a quien la bloqueó.
CREATE TABLE IF NOT EXISTS bloqueos (
  bloqueador_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  bloqueado_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (bloqueador_id, bloqueado_id),
  CHECK (bloqueador_id <> bloqueado_id)
);

-- Cada persona puede reportar a la misma cuenta una sola vez.
CREATE TABLE IF NOT EXISTS reportes (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  reportante_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  reportado_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  motivo VARCHAR(30) NOT NULL CHECK (motivo IN ('acoso','perfil_falso','estafa','contenido_inapropiado','otro')),
  detalle TEXT CHECK (char_length(detalle) <= 500),
  creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (reportante_id, reportado_id),
  CHECK (reportante_id <> reportado_id)
);

-- Cuentas que llegaron al límite de reportes de personas distintas.
CREATE TABLE IF NOT EXISTS alertas_cuenta (
  usuario_id INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE,
  activada_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_bloqueos_bloqueado ON bloqueos(bloqueado_id);
CREATE INDEX IF NOT EXISTS idx_reportes_reportado ON reportes(reportado_id);
CREATE INDEX IF NOT EXISTS idx_reportes_reportante ON reportes(reportante_id, creado_en);

CREATE INDEX IF NOT EXISTS idx_perfiles_zona ON perfiles(zona_id);
CREATE INDEX IF NOT EXISTS idx_recomendaciones_usuario ON recomendaciones(usuario_id);
CREATE INDEX IF NOT EXISTS idx_recomendaciones_candidato ON recomendaciones(candidato_id);
CREATE INDEX IF NOT EXISTS idx_explicaciones_par ON explicaciones_ia(usuario_id, candidato_id);
