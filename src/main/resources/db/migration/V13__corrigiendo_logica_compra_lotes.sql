ALTER TABLE detalle_compra DROP CONSTRAINT fk_detalle_compra_lote;
ALTER TABLE detalle_compra RENAME COLUMN id_lote TO id_producto;
ALTER TABLE detalle_compra ADD CONSTRAINT fk_detalle_compra_producto
    FOREIGN KEY (id_producto) REFERENCES producto (id_producto) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE detalle_recepcion ADD COLUMN id_lote BIGINT;
ALTER TABLE detalle_recepcion ALTER COLUMN id_lote SET NOT NULL;
ALTER TABLE detalle_recepcion ADD CONSTRAINT fk_detalle_recepcion_lote
    FOREIGN KEY (id_lote) REFERENCES lote (id_lote) ON UPDATE CASCADE ON DELETE RESTRICT;