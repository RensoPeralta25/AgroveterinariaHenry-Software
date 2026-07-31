ALTER TABLE abono_prestamo
ADD COLUMN id_empleado_registrador BIGINT;

ALTER TABLE abono_prestamo
ADD CONSTRAINT fk_abono_prestamo_empleado
FOREIGN KEY (id_empleado_registrador) REFERENCES empleado(id_empleado);

ALTER TABLE abono_anticipo
ADD COLUMN id_empleado_registrador BIGINT;

ALTER TABLE abono_anticipo
ADD CONSTRAINT fk_abono_anticipo_empleado
FOREIGN KEY (id_empleado_registrador) REFERENCES empleado(id_empleado);