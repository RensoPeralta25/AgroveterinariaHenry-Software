CREATE TABLE corrida_nomina (
    id_corrida    BIGSERIAL PRIMARY KEY,
    periodo       CHARACTER VARYING NOT NULL,
    fecha_emision DATE NOT NULL,
    estado        CHARACTER VARYING NOT NULL DEFAULT 'PENDIENTE'
);

ALTER TABLE nomina ADD COLUMN id_corrida BIGINT;
ALTER TABLE nomina ADD CONSTRAINT fk_nomina_corrida
    FOREIGN KEY (id_corrida) REFERENCES corrida_nomina(id_corrida);