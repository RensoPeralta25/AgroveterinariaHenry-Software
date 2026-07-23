ALTER TABLE empleado ADD COLUMN status VARCHAR(30);

UPDATE empleado
SET status = CASE
    WHEN activo = TRUE THEN 'ACTIVO'
    ELSE 'INACTIVO'
END;

ALTER TABLE empleado ALTER COLUMN status SET NOT NULL;

ALTER TABLE empleado DROP COLUMN activo;
ALTER TABLE empleado DROP COLUMN prorratear_embargos;