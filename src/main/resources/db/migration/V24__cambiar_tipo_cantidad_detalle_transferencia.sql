ALTER TABLE detalle_transferencia
    ALTER COLUMN cantidad TYPE DECIMAL(12,2) USING cantidad::numeric;
