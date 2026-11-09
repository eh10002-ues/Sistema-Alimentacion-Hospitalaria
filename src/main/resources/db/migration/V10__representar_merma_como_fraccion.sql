-- Convierte el porcentaje histórico (0 a 100) en fracción (0 a 1).
-- Un insumo con merma histórica de 100% no es válido para la fórmula y hará
-- fallar la restricción final; debe corregirse antes de aplicar esta migración.
ALTER TABLE insumos
    DROP CHECK ck_insumos_merma;

ALTER TABLE insumos
    MODIFY COLUMN merma DECIMAL(5, 4) NULL DEFAULT 0.0000;

UPDATE insumos
SET merma = merma / 100;

ALTER TABLE insumos
    ADD CONSTRAINT ck_insumos_merma
        CHECK (merma IS NULL OR (merma >= 0 AND merma < 1));
