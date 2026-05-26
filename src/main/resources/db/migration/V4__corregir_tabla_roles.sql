ALTER TABLE empleado_rol DROP CONSTRAINT empleado_rol_pkey;

ALTER TABLE empleado_rol DROP CONSTRAINT uq_empleado_rol;

ALTER TABLE empleado_rol DROP COLUMN id_empleado_rol;

ALTER TABLE empleado_rol ADD CONSTRAINT empleado_rol_pkey PRIMARY KEY (id_empleado, rol);
