ALTER TABLE detalle_venta ADD COLUMN id_lote BIGINT;

ALTER TABLE detalle_venta
    ADD CONSTRAINT fk_detalle_venta_lote
        FOREIGN KEY (id_lote) REFERENCES lote (id_lote);