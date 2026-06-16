ALTER TABLE "public"."transferencia" DROP CONSTRAINT "chk_transferencia_estado";
ALTER TABLE "public"."transferencia" ADD CONSTRAINT "chk_transferencia_estado"
    CHECK (estado IN ('BORRADOR', 'PENDIENTE_DESPACHO', 'DESPACHADA_PARCIAL', 'EN_TRANSITO', 'RECIBIDA_PARCIAL', 'COMPLETADA', 'CANCELADA'));

ALTER TABLE "public"."despacho" ADD COLUMN "id_transferencia" BIGINT;
ALTER TABLE "public"."despacho" ADD CONSTRAINT "fk_despacho_transferencia" FOREIGN KEY ("id_transferencia") REFERENCES "public"."transferencia"("id_transferencia") ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE "public"."detalle_despacho" ALTER COLUMN "id_detalle_venta" DROP NOT NULL;
ALTER TABLE "public"."detalle_despacho" ADD COLUMN "id_detalle_transferencia" BIGINT;
ALTER TABLE "public"."detalle_despacho" ADD CONSTRAINT "fk_detalle_despacho_transferencia" FOREIGN KEY ("id_detalle_transferencia") REFERENCES "public"."detalle_transferencia"("id_detalle_transferencia") ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE "public"."detalle_despacho" ALTER COLUMN "cantidad" TYPE numeric(12,2);

ALTER TABLE "public"."detalle_despacho" ADD CONSTRAINT "chk_detalle_despacho_origen" CHECK (
    (id_detalle_venta IS NOT NULL AND id_detalle_transferencia IS NULL) OR
    (id_detalle_venta IS NULL AND id_detalle_transferencia IS NOT NULL)
    );

ALTER TABLE "public"."recepcion" ADD COLUMN "id_transferencia" BIGINT;
ALTER TABLE "public"."recepcion" ADD CONSTRAINT "fk_recepcion_transferencia" FOREIGN KEY ("id_transferencia") REFERENCES "public"."transferencia"("id_transferencia") ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE "public"."detalle_recepcion" ALTER COLUMN "id_detalle_compra" DROP NOT NULL;
ALTER TABLE "public"."detalle_recepcion" ADD COLUMN "id_detalle_transferencia" BIGINT;
ALTER TABLE "public"."detalle_recepcion" ADD CONSTRAINT "fk_detalle_recepcion_transferencia" FOREIGN KEY ("id_detalle_transferencia") REFERENCES "public"."detalle_transferencia"("id_detalle_transferencia") ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE "public"."detalle_recepcion" ADD CONSTRAINT "chk_detalle_recepcion_origen" CHECK (
    (id_detalle_compra IS NOT NULL AND id_detalle_transferencia IS NULL) OR
    (id_detalle_compra IS NULL AND id_detalle_transferencia IS NOT NULL)
    );