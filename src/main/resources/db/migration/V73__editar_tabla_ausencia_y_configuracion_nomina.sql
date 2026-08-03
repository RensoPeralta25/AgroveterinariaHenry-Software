ALTER TABLE ausencia ALTER COLUMN fecha_fin DROP NOT NULL;
ALTER TABLE ausencia ADD COLUMN estado_registro VARCHAR(20) NOT NULL DEFAULT 'CERRADA';
ALTER TABLE ausencia ADD COLUMN nombre_archivo VARCHAR(255) NULL;

INSERT INTO configuracion_nomina (clave, valor, descripcion)
VALUES ('PLAZO_JUSTIFICACION_HORAS', '48', 'Horas límite para entregar justificación de la ausencia');