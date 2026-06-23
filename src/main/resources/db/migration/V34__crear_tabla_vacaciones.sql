CREATE TABLE vacacion_empleado (
    id BIGSERIAL PRIMARY KEY,
    empleado_id BIGINT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    cantidad_dias INT NOT NULL,
    pagado_por_adelantado BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT fk_vacacion_empleado FOREIGN KEY (empleado_id) REFERENCES empleado (id)
);