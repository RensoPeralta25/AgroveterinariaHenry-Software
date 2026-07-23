ALTER TABLE cobro
    ADD COLUMN banco_origen VARCHAR(80),
    ADD COLUMN titular_transferencia VARCHAR(150),
    ADD COLUMN referencia_transferencia VARCHAR(100),
    ADD COLUMN comprobante_transferencia BYTEA,
    ADD COLUMN nombre_comprobante VARCHAR(255),
    ADD COLUMN tipo_contenido_comprobante VARCHAR(100),
    ADD COLUMN fecha_confirmacion_transferencia TIMESTAMP;

CREATE UNIQUE INDEX uq_cobro_referencia_transferencia
    ON cobro (LOWER(referencia_transferencia))
    WHERE referencia_transferencia IS NOT NULL;
