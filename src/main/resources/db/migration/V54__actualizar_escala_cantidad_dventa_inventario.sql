ALTER TABLE detalle_venta ALTER COLUMN cantidad TYPE numeric(14, 4);
ALTER TABLE detalle_dev_venta ALTER COLUMN cantidad_devuelta TYPE numeric(14, 4);
ALTER TABLE inventario ALTER COLUMN cantidad_actual TYPE numeric(14, 4);