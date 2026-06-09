ALTER TABLE cobro ADD COLUMN id_venta BIGINT;

ALTER TABLE cobro
    ADD CONSTRAINT fk_cobro_venta
    FOREIGN KEY (id_venta) REFERENCES venta (id_venta)
    ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE INDEX idx_cobro_venta ON cobro (id_venta);
