CREATE TABLE anticipos_salario (
    id BIGSERIAL PRIMARY KEY,
    id_empleado BIGINT NOT NULL,
    fecha_registro DATE NOT NULL,
    monto_original NUMERIC(10, 2) NOT NULL,
    cuota_descuento NUMERIC(10, 2) NOT NULL,
    monto_descontado NUMERIC(10, 2) DEFAULT 0.00 NOT NULL,
    saldo_pendiente NUMERIC(10, 2) NOT NULL,
    estado VARCHAR(20) DEFAULT 'ACTIVO' NOT NULL,
    CONSTRAINT fk_anticipos_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id_empleado)
);