DELETE FROM configuracion_nomina
WHERE clave IN (
    'ANTICIPO_RIESGO_ALTO_MULTIPLICADOR',
    'ANTICIPO_RIESGO_ALTO_PORCENTAJE',
    'ANTICIPO_RIESGO_MEDIO_MULTIPLICADOR',
    'ANTICIPO_RIESGO_MEDIO_PORCENTAJE'
);

INSERT INTO configuracion_nomina (clave, valor, descripcion)
VALUES ('LIMITE_EMBARGO_PORCENTAJE', 0.50, 'Límite máximo legal embargable sobre el salario neto');

ALTER TABLE embargo_salarial
DROP COLUMN monto_descuento,
DROP COLUMN activo;

ALTER TABLE embargo_salarial
RENAME COLUMN tipo TO tipo_embargo;

ALTER TABLE embargo_salarial
ADD COLUMN monto_cuota_ordinaria DECIMAL(12,2) NOT NULL DEFAULT 0.00,
ADD COLUMN saldo_pendiente_mora DECIMAL(12,2) NOT NULL DEFAULT 0.00,
ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO';

CREATE TABLE cuota_extra_embargo (
    id BIGSERIAL PRIMARY KEY,
    embargo_salarial_id BIGINT NOT NULL,
    mes_aplicacion INT NOT NULL CHECK (mes_aplicacion >= 1 AND mes_aplicacion <= 12),
    monto_extra DECIMAL(12,2) NOT NULL,
    concepto VARCHAR(100) NOT NULL,
    CONSTRAINT fk_cuota_extra_embargo
    FOREIGN KEY (embargo_salarial_id)
    REFERENCES embargo_salarial (id_embargo)
    ON DELETE CASCADE
);

CREATE INDEX idx_cuota_extra_embargo_id ON cuota_extra_embargo(embargo_salarial_id);