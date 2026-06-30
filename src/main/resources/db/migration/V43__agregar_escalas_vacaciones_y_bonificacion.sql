INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
    ('DIAS_DESCANSO_VACACIONES', '14', 'Tope de días laborables de descanso por vacaciones'),
    ('ANIOS_VACACIONES_SENIOR', '5', 'Años de antigüedad requeridos para el pago de 18 días'),
    ('DIAS_PAGO_VACACIONES_BASICO', '14', 'Días de salario a pagar por vacaciones (menor a 5 años)'),
    ('DIAS_PAGO_VACACIONES_SENIOR', '18', 'Días de salario a pagar por vacaciones (5 años o más)'),
    ('ANIOS_BONIFICACION_SENIOR', '3', 'Años de antigüedad requeridos para el tope máximo de bonificación');

ALTER TABLE vacacion_empleado RENAME COLUMN cantidad_dias TO cantidad_dias_descanso;
ALTER TABLE vacacion_empleado ADD COLUMN cantidad_dias_a_pagar INTEGER;