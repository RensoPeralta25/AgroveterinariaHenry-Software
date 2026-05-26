ALTER TABLE producto RENAME COLUMN precio_unitario TO precio_fraccion;
ALTER TABLE producto RENAME COLUMN presentacion TO contenido_por_empaque;
ALTER TABLE producto RENAME COLUMN unidad_medida TO unidad_fraccion;

ALTER TABLE producto ALTER COLUMN precio_fraccion DROP NOT NULL;
ALTER TABLE producto ALTER COLUMN contenido_por_empaque DROP NOT NULL;
ALTER TABLE producto ALTER COLUMN unidad_fraccion DROP NOT NULL;

ALTER TABLE producto ADD COLUMN precio_empaque NUMERIC(10, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE producto ADD COLUMN unidad_empaque VARCHAR(50) NOT NULL DEFAULT 'UNIDAD_COMPLETA';
ALTER TABLE producto ADD COLUMN permite_fraccionamiento BOOLEAN NOT NULL DEFAULT FALSE;