-- =====================================================================
-- HablaBar - Índices para la búsqueda de chats y participaciones
-- =====================================================================

-- Búsqueda de chats activos cercanos: filtra por caducidad y por un
-- rectángulo de coordenadas alrededor del usuario.
CREATE INDEX idx_chats_caducidad_origen
    ON chats (fecha_caducidad, latitud_origen, longitud_origen);

-- Chats en los que un usuario participa actualmente (RF-14).
CREATE INDEX idx_participaciones_usuario_activa
    ON participaciones (usuario_id)
    WHERE fecha_salida IS NULL;