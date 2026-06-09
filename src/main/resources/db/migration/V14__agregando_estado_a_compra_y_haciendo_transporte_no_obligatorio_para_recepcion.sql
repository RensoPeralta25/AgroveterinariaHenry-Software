ALTER TABLE recepcion ALTER COLUMN id_transporte DROP NOT NULL;

ALTER TABLE compra ADD COLUMN estado_recepcion VARCHAR(20) DEFAULT 'PENDIENTE' NOT NULL;

ALTER TABLE compra ADD CONSTRAINT chk_compra_estado_recepcion CHECK (estado_recepcion IN ('BORRADOR', 'PENDIENTE', 'PARCIAL', 'RECIBIDA'));