CREATE TABLE prestamo_empleado (
    id_prestamo BIGSERIAL PRIMARY KEY,
    id_empleado BIGINT NOT NULL,
    monto_total DECIMAL(19, 2) NOT NULL,
    balance_pendiente DECIMAL(19, 2) NOT NULL,
    cuota_periodica DECIMAL(19, 2) NOT NULL,
    fecha_aprobacion DATE NOT NULL,
    concepto VARCHAR(255),
    estado VARCHAR(50) NOT NULL,
    CONSTRAINT fk_prestamo_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado)
);

CREATE TABLE embargo_salarial (
    id_embargo BIGSERIAL PRIMARY KEY,
    id_empleado BIGINT NOT NULL,
    entidad_demandante VARCHAR(255) NOT NULL,
    monto_descuento DECIMAL(19, 2) NOT NULL,
    fecha_notificacion DATE NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_embargo_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado)
);