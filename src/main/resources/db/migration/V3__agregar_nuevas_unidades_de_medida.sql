ALTER TABLE producto DROP CONSTRAINT producto_unidad_medida_check;

ALTER TABLE producto ADD CONSTRAINT producto_unidad_medida_check
    CHECK (unidad_medida IN ('LIBRA', 'QUINTAL', 'MILIGRAMO', 'GRAMO', 'KILOGRAMO', 'CC', 'LITRO', 'MILILITRO', 'UNIDAD'));