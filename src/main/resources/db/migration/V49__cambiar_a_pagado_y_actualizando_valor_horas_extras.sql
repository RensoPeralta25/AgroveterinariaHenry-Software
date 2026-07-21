ALTER TABLE vacacion_empleado
RENAME COLUMN pagado_por_adelantado TO pagado;


UPDATE configuracion_nomina
SET valor = 200
WHERE clave = 'HORA_EXTRA_VALOR_FIJO'