ALTER TABLE producto
    ADD COLUMN IF NOT EXISTS porcentaje_impuesto NUMERIC(5,2);

UPDATE producto
SET porcentaje_impuesto = 0.00
WHERE porcentaje_impuesto IS NULL;

ALTER TABLE producto
    ALTER COLUMN porcentaje_impuesto TYPE NUMERIC(5,2)
        USING porcentaje_impuesto::NUMERIC(5,2),
    ALTER COLUMN porcentaje_impuesto SET DEFAULT 0.00,
    ALTER COLUMN porcentaje_impuesto SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_producto_porcentaje_impuesto'
          AND conrelid = 'producto'::regclass
    ) THEN
        ALTER TABLE producto
            ADD CONSTRAINT chk_producto_porcentaje_impuesto
                CHECK (porcentaje_impuesto >= 0.00 AND porcentaje_impuesto <= 100.00);
    END IF;
END
$$;
