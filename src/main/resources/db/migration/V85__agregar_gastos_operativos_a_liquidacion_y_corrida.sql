ALTER TABLE liquidacion_empleado ADD COLUMN id_gasto_neto BIGINT;
ALTER TABLE liquidacion_empleado ADD COLUMN id_gasto_embargo BIGINT;

ALTER TABLE liquidacion_empleado ADD CONSTRAINT fk_liq_gasto_neto FOREIGN KEY (id_gasto_neto) REFERENCES gasto_operativo(id_gasto);
ALTER TABLE liquidacion_empleado ADD CONSTRAINT fk_liq_gasto_embargo FOREIGN KEY (id_gasto_embargo) REFERENCES gasto_operativo(id_gasto);

CREATE TABLE corrida_gasto_embargo (
    id_corrida BIGINT NOT NULL,
    id_gasto BIGINT NOT NULL,
    PRIMARY KEY (id_corrida, id_gasto),
    CONSTRAINT fk_cge_corrida FOREIGN KEY (id_corrida) REFERENCES corrida_nomina(id_corrida),
    CONSTRAINT fk_cge_gasto FOREIGN KEY (id_gasto) REFERENCES gasto_operativo(id_gasto)
);