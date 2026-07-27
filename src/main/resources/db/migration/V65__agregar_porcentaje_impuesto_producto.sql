ALTER TABLE producto
    ADD COLUMN porcentaje_impuesto NUMERIC(5,2) DEFAULT 0.00 NOT NULL;

ALTER TABLE producto
    ADD CONSTRAINT chk_producto_porcentaje_impuesto
        CHECK (porcentaje_impuesto >= 0.00 AND porcentaje_impuesto <= 100.00);
