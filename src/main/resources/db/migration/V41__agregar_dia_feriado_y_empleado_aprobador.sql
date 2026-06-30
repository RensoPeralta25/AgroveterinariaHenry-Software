
CREATE TABLE dia_feriado (
    id BIGSERIAL PRIMARY KEY,
    fecha DATE NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL
);

ALTER TABLE vacacion_empleado
ADD COLUMN id_empleado_aprobador BIGINT;

ALTER TABLE vacacion_empleado
ADD CONSTRAINT fk_vacacion_empleado_aprobador
FOREIGN KEY (id_empleado_aprobador) REFERENCES empleado(id_empleado);