ALTER TABLE embargo_salarial DROP COLUMN IF EXISTS version;
ALTER TABLE prestamo_empleado DROP COLUMN IF EXISTS version;

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('MAX_HORAS_EXTRAS_SEMANAL', 24.00, 'Límite máximo de horas extras permitidas por semana'),
('MAX_HORAS_EXTRAS_QUINCENAL', 48.00, 'Límite máximo de horas extras permitidas por quincena'),
('MAX_HORAS_EXTRAS_MENSUAL', 104.00, 'Límite máximo de horas extras permitidas por mes');