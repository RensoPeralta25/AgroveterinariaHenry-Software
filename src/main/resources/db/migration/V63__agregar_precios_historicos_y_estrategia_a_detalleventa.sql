ALTER TABLE detalle_venta
    ADD COLUMN estrategia_precio VARCHAR(50),
ADD COLUMN precio_empaque_historico NUMERIC(14,6),
ADD COLUMN precio_fraccion_historico NUMERIC(14,6);