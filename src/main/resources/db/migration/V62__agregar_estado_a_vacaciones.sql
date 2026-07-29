ALTER TABLE vacacion_empleado ADD COLUMN estado VARCHAR(50);

ALTER TABLE vacacion_empleado ALTER COLUMN estado SET NOT NULL;

ALTER TABLE vacacion_empleado ALTER COLUMN id_empleado_aprobador DROP NOT NULL;

ALTER TABLE vacacion_empleado DROP COLUMN pagado;