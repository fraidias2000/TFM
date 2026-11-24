-- =====================================================================
-- HablaBar - Identificador de cliente de los mensajes (RNF-06)
-- =====================================================================

-- El cliente genera un identificador para cada mensaje y lo repite si reenvía
-- el mismo mensaje tras un fallo de conexión. El índice único impide guardarlo
-- dos veces.
ALTER TABLE mensajes
    ADD COLUMN id_cliente UUID NOT NULL;

CREATE UNIQUE INDEX uq_mensajes_id_cliente
    ON mensajes (id_cliente);