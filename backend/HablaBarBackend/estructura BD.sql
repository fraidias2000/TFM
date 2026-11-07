BEGIN;

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
    CHECK (fecha_caducidad = fecha_creacion + INTERVAL '1 hour')
);

CREATE TABLE participaciones (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id    UUID NOT NULL REFERENCES usuarios(id),
    chat_id       UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    fecha_entrada TIMESTAMPTZ NOT NULL DEFAULT now(),
    fecha_salida  TIMESTAMPTZ,
    UNIQUE (usuario_id, chat_id),
    CHECK (fecha_salida IS NULL OR fecha_salida >= fecha_entrada)
);

CREATE TABLE mensajes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    participacion_id UUID NOT NULL REFERENCES participaciones(id) ON DELETE CASCADE,
    contenido        VARCHAR(1000) NOT NULL CHECK (btrim(contenido) <> ''),
    fecha_envio      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_participaciones_chat ON participaciones (chat_id);
CREATE INDEX idx_mensajes_participacion_fecha ON mensajes (participacion_id, fecha_envio);

COMMIT;