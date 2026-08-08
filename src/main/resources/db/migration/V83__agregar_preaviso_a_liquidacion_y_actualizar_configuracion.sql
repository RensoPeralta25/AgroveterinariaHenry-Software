ALTER TABLE liquidacion_empleado ADD COLUMN preaviso_trabajado BOOLEAN DEFAULT FALSE;

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('PREAVISO_MESES_MINIMO_TRAMO_1', 3, 'Meses mínimos para generar preaviso (Tramo 1)'),
('PREAVISO_MESES_MINIMO_TRAMO_2', 6, 'Meses mínimos para preaviso (Tramo 2)'),
('CESANTIA_MESES_MINIMO_TRAMO_1', 3, 'Meses mínimos para generar cesantía (Tramo 1)'),
('CESANTIA_MESES_MINIMO_TRAMO_2', 6, 'Meses mínimos para cesantía (Tramo 2)'),
('CESANTIA_ANIOS_MINIMO_TRAMO_4', 5, 'Años mínimos para cesantía senior (Tramo 4)');