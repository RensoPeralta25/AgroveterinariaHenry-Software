ALTER TABLE liquidacion_empleado
ADD COLUMN dias_preaviso_trabajados INTEGER DEFAULT 0;

ALTER TABLE liquidacion_empleado
DROP COLUMN preaviso_trabajado;