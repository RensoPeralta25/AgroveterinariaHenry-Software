ALTER TABLE embargo_salarial
ADD COLUMN tipo VARCHAR(80);

ALTER TABLE embargo_salarial
ALTER COLUMN tipo SET NOT NULL;