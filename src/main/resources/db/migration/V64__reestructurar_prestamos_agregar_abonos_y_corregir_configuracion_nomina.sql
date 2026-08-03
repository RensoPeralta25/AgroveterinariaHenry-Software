ALTER TABLE prestamo_empleado RENAME COLUMN monto_total TO monto_capital;
ALTER TABLE prestamo_empleado RENAME COLUMN balance_pendiente TO balance_capital_pendiente;
ALTER TABLE prestamo_empleado ADD COLUMN tasa_interes DECIMAL(5,2) NOT NULL DEFAULT 0.00;
ALTER TABLE prestamo_empleado ADD COLUMN plazo_meses INT NOT NULL DEFAULT 1;
ALTER TABLE prestamo_empleado ADD COLUMN cuotas_pagadas INT NOT NULL DEFAULT 0;

CREATE TABLE abono_prestamo (
    id BIGSERIAL PRIMARY KEY,
    id_prestamo BIGINT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_abono DATE NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    tipo_recalculo VARCHAR(50) NOT NULL,
    CONSTRAINT fk_abono_prestamo FOREIGN KEY (id_prestamo) REFERENCES prestamo_empleado(id_prestamo)
);


CREATE TABLE abono_anticipo (
    id BIGSERIAL PRIMARY KEY,
    id_anticipo BIGINT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    fecha_abono DATE NOT NULL,
    metodo_pago VARCHAR(50) NOT NULL,
    CONSTRAINT fk_abono_anticipo FOREIGN KEY (id_anticipo) REFERENCES anticipos_salario(id)
);

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('PRESTAMO_FACTOR_MINIMO_SALARIO', 0.5, 'Factor para el monto mínimo de préstamo (0.5 = 50% del salario neto)'),
('PRESTAMO_FACTOR_MAXIMO_SALARIO', 2.0, 'Factor para el monto máximo de préstamo (2.0 = 2 veces el salario neto)'),
('PRESTAMO_PLAZO_MAXIMO_MESES', 12, 'Plazo máximo en meses permitido para saldar un préstamo');

UPDATE configuracion_nomina
SET
    clave = 'DIVISOR_LIMITE_CUOTA_PRESTAMO',
    valor = 6,
    descripcion = 'Divisor para calcular la porción máxima del salario permitida para la cuota de un préstamo'
WHERE clave = 'PORCENTAJE_MAXIMO_PRESTAMO';

DELETE FROM configuracion_nomina
WHERE clave = 'DIVISOR_LIMITE_EMBARGO';