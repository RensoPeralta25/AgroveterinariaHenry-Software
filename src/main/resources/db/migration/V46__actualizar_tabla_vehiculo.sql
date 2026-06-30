ALTER TABLE vehiculo RENAME COLUMN capacidad_carga TO capacidad_carga_kg;
ALTER TABLE vehiculo ALTER COLUMN capacidad_carga_kg TYPE NUMERIC(10, 2);

ALTER TABLE vehiculo ADD COLUMN anio_fabricacion INT NOT NULL DEFAULT 2026;
ALTER TABLE vehiculo ADD COLUMN tipo_combustible VARCHAR(30) NOT NULL DEFAULT 'Diésel';
ALTER TABLE vehiculo ADD COLUMN estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE';

ALTER TABLE vehiculo ADD COLUMN fecha_vencimiento_seguro DATE;
ALTER TABLE vehiculo ADD COLUMN fecha_vencimiento_matricula DATE;

ALTER TABLE vehiculo ALTER COLUMN marca TYPE VARCHAR(50);
ALTER TABLE vehiculo ALTER COLUMN modelo TYPE VARCHAR(50);