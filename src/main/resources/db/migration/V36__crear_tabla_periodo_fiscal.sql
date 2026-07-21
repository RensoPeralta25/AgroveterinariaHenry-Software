CREATE TABLE periodo_fiscal (
    id BIGSERIAL PRIMARY KEY,
    anio INT NOT NULL UNIQUE,
    fecha_inicio DATE NOT NULL,
    fecha_cierre DATE NOT NULL,
    cerrado BOOLEAN NOT NULL DEFAULT FALSE
);

INSERT INTO periodo_fiscal (anio, fecha_inicio, fecha_cierre, cerrado)
VALUES (2026, '2026-01-01', '2026-12-31', FALSE);

ALTER TABLE corrida_nomina
ADD COLUMN periodo_fiscal_id BIGINT;

ALTER TABLE corrida_nomina
ADD CONSTRAINT fk_corrida_periodo
FOREIGN KEY (periodo_fiscal_id) REFERENCES periodo_fiscal(id);