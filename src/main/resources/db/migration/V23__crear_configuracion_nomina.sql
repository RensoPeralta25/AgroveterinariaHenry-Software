CREATE TABLE configuracion_nomina (
    id          BIGSERIAL PRIMARY KEY,
    clave       CHARACTER VARYING NOT NULL UNIQUE,
    valor       NUMERIC NOT NULL,
    descripcion CHARACTER VARYING
);

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('AFP_PORCENTAJE',          0.0287,    'Porcentaje AFP empleado'),
('AFP_TOPE',                387050.00, 'Tope máximo AFP (20 SMN)'),
('SFS_PORCENTAJE',          0.0304,    'Porcentaje SFS empleado'),
('SFS_TOPE',                193525.00, 'Tope máximo SFS (10 SMN)'),
('HORA_EXTRA_VALOR_FIJO',   100.00,      'Valor fijo por hora extra'),
('ISR_TRAMO_1_LIMITE',      416220.00, 'ISR tramo 1 límite anual'),
('ISR_TRAMO_2_LIMITE',      624329.00, 'ISR tramo 2 límite anual'),
('ISR_TRAMO_3_LIMITE',      867123.00, 'ISR tramo 3 límite anual'),
('ISR_TRAMO_1_PORCENTAJE',  0.15,      'ISR porcentaje tramo 1'),
('ISR_TRAMO_2_BASE',        31216.00,  'ISR base fija tramo 2'),
('ISR_TRAMO_2_PORCENTAJE',  0.20,      'ISR porcentaje tramo 2'),
('ISR_TRAMO_3_BASE',        79776.00,  'ISR base fija tramo 3'),
('ISR_TRAMO_3_PORCENTAJE',  0.25,      'ISR porcentaje tramo 3');