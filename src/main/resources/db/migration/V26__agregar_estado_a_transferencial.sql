ALTER TABLE "public"."transferencia"
    ADD COLUMN "estado" character varying(30) NOT NULL DEFAULT 'PENDIENTE_DESPACHO';

ALTER TABLE "public"."transferencia"
    ADD CONSTRAINT "chk_transferencia_estado"
        CHECK (estado IN ('BORRADOR', 'PENDIENTE_DESPACHO', 'EN_TRANSITO', 'RECIBIDA_PARCIAL', 'COMPLETADA', 'CANCELADA'));