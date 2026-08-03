CREATE TABLE ausencia (
    id_ausencia BIGSERIAL PRIMARY KEY,
    id_empleado BIGINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    tipo_ausencia VARCHAR(50) NOT NULL,
    documento_adjunto BYTEA,

    aplicada_en_nomina BOOLEAN DEFAULT FALSE,
    id_nomina_aplicada BIGINT,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ausencia_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id_empleado),
    CONSTRAINT fk_ausencia_nomina FOREIGN KEY (id_nomina_aplicada) REFERENCES nomina(id_nomina)
);

CREATE TABLE historial_devengado_anual (
    id_historial BIGSERIAL PRIMARY KEY,
    id_empleado BIGINT NOT NULL,
    anio INT NOT NULL,
    mes INT NOT NULL,
    monto_devengado_real NUMERIC(12, 2) NOT NULL,

    CONSTRAINT fk_historial_empleado FOREIGN KEY (id_empleado) REFERENCES empleado(id_empleado),
    UNIQUE (id_empleado, anio, mes)
);