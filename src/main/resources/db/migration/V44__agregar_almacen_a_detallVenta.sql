ALTER TABLE detalle_venta ADD COLUMN id_almacen BIGINT;

ALTER TABLE detalle_venta
    ADD CONSTRAINT fk_detalle_venta_almacen
        FOREIGN KEY (id_almacen) REFERENCES almacen (id_almacen);