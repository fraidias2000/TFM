-- =====================================================================
-- HablaBar - Esquema inicial de la base de datos (PostgreSQL 17)
-- =====================================================================

-- ---------------------------------------------------------------------
-- USUARIOS
-- ---------------------------------------------------------------------
CREATE TABLE usuarios (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alias            TEXT NOT NULL CHECK (btrim(alias) <> ''),
    correo           TEXT NOT NULL CHECK (btrim(correo) <> ''),
    password_hash    TEXT NOT NULL,
    avisos_activados BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Evita registrar dos cuentas con el mismo correo aunque cambien las mayúsculas.
CREATE UNIQUE INDEX uq_usuarios_correo
    ON usuarios (lower(btrim(correo)));

-- ---------------------------------------------------------------------
-- CHATS
-- ---------------------------------------------------------------------
CREATE TABLE chats (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creador_id       UUID NOT NULL REFERENCES usuarios(id),
    nombre           TEXT NOT NULL CHECK (btrim(nombre) <> ''),
    tema             TEXT,
    latitud_origen   DOUBLE PRECISION NOT NULL
                     CHECK (latitud_origen BETWEEN -90 AND 90),
    longitud_origen  DOUBLE PRECISION NOT NULL
                     CHECK (longitud_origen BETWEEN -180 AND 180),
    fecha_creacion   TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_caducidad  TIMESTAMPTZ NOT NULL DEFAULT (now() + INTERVAL '1 hour'),
    capacidad_maxima SMALLINT NOT NULL DEFAULT 20
                     CHECK (capacidad_maxima BETWEEN 1 AND 20),
    -- RN-06: duración fija de una hora desde la creación.
    CHECK (fecha_caducidad = fecha_creacion + INTERVAL '1 hour')
);

-- ---------------------------------------------------------------------
-- PARTICIPACIONES
-- Cada entrada en un chat crea una participación nueva. Al salir se
-- rellena fecha_salida. Si el usuario vuelve a entrar, se crea otra fila.
-- ---------------------------------------------------------------------
CREATE TABLE participaciones (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id    UUID NOT NULL REFERENCES usuarios(id),
    chat_id       UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    fecha_entrada TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_salida  TIMESTAMPTZ,
    CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_entrada)
);

-- Un usuario solo puede tener una participación ACTIVA por chat,
-- pero puede tener varias cerradas (entradas y salidas sucesivas).
-- Sustituye al antiguo UNIQUE (usuario_id, chat_id). RF-11, RF-12.
CREATE UNIQUE INDEX uq_participacion_activa
    ON participaciones (usuario_id, chat_id)
    WHERE fecha_salida IS NULL;

-- ---------------------------------------------------------------------
-- MENSAJES
-- El autor y el chat se obtienen a través de la participación.
-- ---------------------------------------------------------------------
CREATE TABLE mensajes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participacion_id UUID NOT NULL REFERENCES participaciones(id) ON DELETE CASCADE,
    contenido        VARCHAR(1000) NOT NULL CHECK (btrim(contenido) <> ''),
    fecha_envio      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------
-- ÍNDICES
-- ---------------------------------------------------------------------
CREATE INDEX idx_participaciones_chat ON participaciones (chat_id);
CREATE INDEX idx_mensajes_participacion_fecha ON mensajes (participacion_id, fecha_envio);
