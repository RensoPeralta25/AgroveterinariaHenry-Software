CREATE TABLE liquidacion_empleado (
    id BIGSERIAL PRIMARY KEY,
    empleado_id BIGINT NOT NULL,
    fecha_liquidacion DATE NOT NULL,
    motivo_salida VARCHAR(30) NOT NULL,

    monto_regalia NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    monto_vacaciones NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    monto_preaviso NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    monto_cesantia NUMERIC(12,2) NOT NULL DEFAULT 0.00,

    descuento_prestamos NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    descuento_anticipos NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    descuento_embargos NUMERIC(12,2) NOT NULL DEFAULT 0.00,

    total_ingresos NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total_deducciones NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    monto_neto NUMERIC(12,2) NOT NULL DEFAULT 0.00,

    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observaciones TEXT,

    CONSTRAINT fk_liquidacion_empleado FOREIGN KEY (empleado_id) REFERENCES empleado (id_empleado)
);

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('PREAVISO_DIAS_TRAMO_1', 7.00, 'Preaviso Tramo 1'),
('PREAVISO_DIAS_TRAMO_2', 14.00, 'Preaviso Tramo 2'),
('PREAVISO_DIAS_TRAMO_3', 28.00, 'Preaviso Tramo 3');

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('CESANTIA_DIAS_TRAMO_1', 6.00, 'Cesantía Tramo 1'),
('CESANTIA_DIAS_TRAMO_2', 13.00, 'Cesantía Tramo 2'),
('CESANTIA_DIAS_TRAMO_3', 21.00, 'Cesantía Tramo 3'),
('CESANTIA_DIAS_TRAMO_4', 23.00, 'Cesantía Tramo 4');