ALTER TABLE corrida_nomina
ADD COLUMN id_gasto BIGINT;

ALTER TABLE corrida_nomina
ADD CONSTRAINT fk_corrida_nomina_gasto
FOREIGN KEY (id_gasto) REFERENCES gasto_operativo(id_gasto);

ALTER TABLE prestamo_empleado
ADD COLUMN id_gasto BIGINT;

ALTER TABLE prestamo_empleado
ADD CONSTRAINT fk_prestamo_gasto
FOREIGN KEY (id_gasto) REFERENCES gasto_operativo(id_gasto);

ALTER TABLE anticipos_salario
ADD COLUMN id_gasto BIGINT;

ALTER TABLE anticipos_salario
ADD CONSTRAINT fk_anticipo_gasto
FOREIGN KEY (id_gasto) REFERENCES gasto_operativo(id_gasto);