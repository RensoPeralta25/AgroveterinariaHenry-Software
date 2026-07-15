UPDATE configuracion_nomina SET descripcion = 'Tope máximo salarial para el cálculo de AFP' WHERE clave = 'AFP_TOPE';
UPDATE configuracion_nomina SET descripcion = 'Tope máximo salarial para el cálculo de SFS' WHERE clave = 'SFS_TOPE';
UPDATE configuracion_nomina SET descripcion = 'Factor legal estándar para calcular el salario diario' WHERE clave = 'DIVISOR_MENSUAL_DIARIO';
UPDATE configuracion_nomina SET descripcion = 'Límite legal de retención salarial para cuotas de préstamos según el Código de Trabajo' WHERE clave = 'PORCENTAJE_MAXIMO_PRESTAMO';

UPDATE configuracion_nomina SET descripcion = 'Días de salario a pagar por bonificación en la escala básica' WHERE clave = 'BONIFICACION_DIAS_BASE';
UPDATE configuracion_nomina SET descripcion = 'Días de salario a pagar por bonificación en la escala senior' WHERE clave = 'BONIFICACION_DIAS_TOPE';
UPDATE configuracion_nomina SET descripcion = 'Días de salario a pagar por vacaciones en la escala básica' WHERE clave = 'DIAS_PAGO_VACACIONES_BASICO';
UPDATE configuracion_nomina SET descripcion = 'Días de salario a pagar por vacaciones en la escala senior' WHERE clave = 'DIAS_PAGO_VACACIONES_SENIOR';
UPDATE configuracion_nomina SET descripcion = 'Años requeridos para aplicar a la escala senior de bonificación' WHERE clave = 'ANIOS_BONIFICACION_SENIOR';
UPDATE configuracion_nomina SET descripcion = 'Años requeridos para aplicar a la escala senior de vacaciones' WHERE clave = 'ANIOS_VACACIONES_SENIOR';

INSERT INTO configuracion_nomina (clave, valor, descripcion) VALUES
('SALARIO_MINIMO_LEGAL', 16993.20, 'Salario mínimo nacional'),
('ANTICIPO_PORCENTAJE_MAXIMO_MONTO', 0.50, 'Porcentaje máximo sobre el salario base que la empresa autoriza otorgar como anticipo'),
('ANTICIPO_PORCENTAJE_MINIMO_MONTO', 0.10, 'Porcentaje mínimo respecto al salario legal para establecer el monto base de un anticipo'),
('ANTICIPO_DIVISOR_MAXIMO_CUOTA', 6.00, 'Divisor para calcular el tope máximo de la cuota de descuento'),
('ANTICIPO_PLAZO_MAXIMO_MESES', 4.00, 'Cantidad máxima de meses permitida para la amortización y saldo total de un anticipo salarial'),
('ANTICIPO_RIESGO_ALTO_MULTIPLICADOR', 2.00, 'Multiplicador del salario mínimo para definir el límite superior de la banda de riesgo alto'),
('ANTICIPO_RIESGO_ALTO_PORCENTAJE', 0.05, 'Porcentaje máximo de retención de la cuota del anticipo para empleados en la banda de riesgo alto'),
('ANTICIPO_RIESGO_MEDIO_MULTIPLICADOR', 3.00, 'Multiplicador del salario mínimo para definir el límite superior de la banda de riesgo medio'),
('ANTICIPO_RIESGO_MEDIO_PORCENTAJE', 0.10, 'Porcentaje máximo de retención de la cuota del anticipo para empleados en la banda de riesgo medio');