CREATE TABLE vacacion_empleado (
    id BIGSERIAL PRIMARY KEY,
    id_empleado BIGINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    cantidad_dias INT NOT NULL,
    pagado_por_adelantado BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT fk_vacacion_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado)
);