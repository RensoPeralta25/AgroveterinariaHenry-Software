ALTER TABLE empleado_rol DROP CONSTRAINT empleado_rol_rol_check;


ALTER TABLE empleado_rol ADD CONSTRAINT empleado_rol_rol_check
CHECK (rol IN ('ADMINISTRADOR', 'CAJERO', 'VETERINARIO', 'ASISTENTE', 'CONDUCTOR', 'RECURSOS_HUMANOS'));