ALTER TABLE nota_credito
    ADD COLUMN saldo_disponible DECIMAL(12,2),
    ADD COLUMN fecha_emision TIMESTAMP,
    ADD COLUMN motivo VARCHAR(255);

UPDATE nota_credito
SET saldo_disponible = monto,
    fecha_emision = CURRENT_TIMESTAMP
WHERE saldo_disponible IS NULL
   OR fecha_emision IS NULL;

ALTER TABLE nota_credito
    ALTER COLUMN saldo_disponible SET NOT NULL,
    ALTER COLUMN fecha_emision SET NOT NULL;

ALTER TABLE nota_credito
    ADD CONSTRAINT ck_nota_credito_saldo_no_negativo
        CHECK (saldo_disponible >= 0),
    ADD CONSTRAINT ck_nota_credito_saldo_hasta_monto
        CHECK (saldo_disponible <= monto);

ALTER TABLE cobro
    ADD COLUMN id_nota_credito BIGINT;

ALTER TABLE cobro
    ADD CONSTRAINT fk_cobro_nota_credito
        FOREIGN KEY (id_nota_credito) REFERENCES nota_credito (id_nota_credito)
        ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE INDEX idx_nota_credito_saldo_cliente
    ON nota_credito (id_cliente, saldo_disponible);

CREATE INDEX idx_cobro_nota_credito
    ON cobro (id_nota_credito);
