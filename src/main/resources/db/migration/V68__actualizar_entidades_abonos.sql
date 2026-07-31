
ALTER TABLE abono_prestamo
ADD COLUMN banco_origen VARCHAR(80),
ADD COLUMN titular_transferencia VARCHAR(150),
ADD COLUMN referencia_transferencia VARCHAR(100) UNIQUE,
ADD COLUMN comprobante_transferencia BYTEA,
ADD COLUMN nombre_comprobante VARCHAR(255),
ADD COLUMN tipo_contenido_comprobante VARCHAR(100),
ADD COLUMN fecha_confirmacion_transferencia TIMESTAMP;

ALTER TABLE abono_anticipo
ADD COLUMN banco_origen VARCHAR(80),
ADD COLUMN titular_transferencia VARCHAR(150),
ADD COLUMN referencia_transferencia VARCHAR(100) UNIQUE,
ADD COLUMN comprobante_transferencia BYTEA,
ADD COLUMN nombre_comprobante VARCHAR(255),
ADD COLUMN tipo_contenido_comprobante VARCHAR(100),
ADD COLUMN fecha_confirmacion_transferencia TIMESTAMP;