ALTER TABLE detalle_dev_venta
ALTER COLUMN cantidad_devuelta TYPE DECIMAL(12,2);

ALTER TABLE devolucion_venta
    ADD COLUMN monto_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA';

ALTER TABLE devolucion_venta
    ADD COLUMN id_nota_credito BIGINT;

ALTER TABLE devolucion_venta
    ADD CONSTRAINT fk_devolucion_nota_credito
        FOREIGN KEY (id_nota_credito) REFERENCES nota_credito (id_nota_credito);