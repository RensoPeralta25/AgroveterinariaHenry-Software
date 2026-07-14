ALTER TABLE detalle_venta ALTER COLUMN precio_unitario_venta TYPE numeric(14, 6);
ALTER TABLE detalle_venta ALTER COLUMN id_almacen DROP NOT NULL;