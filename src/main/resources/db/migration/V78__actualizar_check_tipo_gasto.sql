ALTER TABLE gasto_operativo
DROP CONSTRAINT gasto_operativo_tipo_gasto_check;

ALTER TABLE gasto_operativo
ADD CONSTRAINT gasto_operativo_tipo_gasto_check
CHECK (tipo_gasto IN ('FIJO', 'VARIABLE', 'NOMINA', 'PRESTAMO_EMPLEADO', 'ANTICIPO_SALARIO'));