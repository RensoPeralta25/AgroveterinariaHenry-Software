-- Datos de demostracion coherentes para AgroVeterinaria Henry.
-- Requisitos:
--   1. Haber ejecutado todas las migraciones Flyway hasta V57.
--   2. Ejecutar una sola vez sobre agroveterinaria_db.
--
-- Usuarios creados (todos usan temporalmente la clave Demo2026!):
--   roberto.admin, carlos.vet, ana.caja, miguel.ruta, patricia.asistente
-- Cambie estas claves desde la aplicacion despues de validar el acceso.

BEGIN;

DO $seed$
DECLARE
    demo_password CONSTANT text := '$2y$10$5QWOkI9Q/D58gxfLnkV5m.vaEXfisx5ivtTCzFj12uWhuv5xJa.6u';

    u_admin bigint; u_vet bigint; u_cajera bigint; u_conductor bigint; u_asistente bigint;
    p_admin bigint; p_vet bigint; p_cajera bigint; p_conductor bigint; p_asistente bigint;
    e_admin bigint; e_vet bigint; e_cajera bigint; e_conductor bigint; e_asistente bigint;

    tc_regular bigint; tc_frecuente bigint; tc_mayorista bigint;
    p_cliente1 bigint; p_cliente2 bigint; p_cliente3 bigint; p_cliente4 bigint; p_cliente5 bigint;
    c1 bigint; c2 bigint; c3 bigint; c4 bigint; c5 bigint;
    m1 bigint; m2 bigint; m3 bigint; m4 bigint; m5 bigint;

    prov1 bigint; prov2 bigint; prov3 bigint;
    prod_alimento bigint; prod_cachorro bigint; prod_vitaminas bigint; prod_antiparasitario bigint;
    prod_shampoo bigint; prod_arena bigint; prod_jeringas bigint; prod_collares bigint;
    srv_consulta bigint; srv_vacuna bigint; srv_peluqueria bigint;
    lote_alimento bigint; lote_cachorro bigint; lote_vitaminas bigint; lote_antiparasitario bigint;
    lote_shampoo bigint; lote_arena bigint; lote_jeringas bigint; lote_collares bigint;
    alm_principal bigint; alm_sucursal bigint;

    gasto_alquiler bigint; gasto_energia bigint; gasto_combustible bigint; gasto_clinico bigint;
    compra1 bigint; compra2 bigint; dc1 bigint; dc2 bigint; dc3 bigint; dc4 bigint;
    venta1 bigint; venta2 bigint; venta3 bigint; dv1 bigint; dv2 bigint; dv3 bigint; dv4 bigint; dv5 bigint;
    nota1 bigint; devolucion1 bigint;
    cita1 bigint; cita2 bigint; cita3 bigint; realizacion1 bigint;

    veh1 bigint; veh2 bigint; ruta1 bigint; ruta2 bigint; transporte1 bigint; transporte2 bigint;
    transferencia1 bigint; dt1 bigint; despacho_transf bigint; recepcion_transf bigint;
    despacho_venta1 bigint; recepcion_compra1 bigint;
    periodo_fiscal_2026 bigint; corrida1 bigint; nomina1 bigint; nomina2 bigint; nomina3 bigint;
BEGIN
    IF EXISTS (SELECT 1 FROM usuario WHERE username = 'roberto.admin') THEN
        RAISE EXCEPTION 'Los datos demo ya fueron cargados (existe roberto.admin).';
    END IF;

    SELECT id_tipo_cliente INTO tc_regular FROM tipo_cliente WHERE nombre_tipo_cliente = 'Regular';
    SELECT id_tipo_cliente INTO tc_frecuente FROM tipo_cliente WHERE nombre_tipo_cliente = 'Frecuente';
    SELECT id_tipo_cliente INTO tc_mayorista FROM tipo_cliente WHERE nombre_tipo_cliente = 'Mayorista';
    IF tc_regular IS NULL OR tc_frecuente IS NULL OR tc_mayorista IS NULL THEN
        RAISE EXCEPTION 'No existen los tipos de cliente iniciales de Flyway.';
    END IF;

    -- Equipo de trabajo y accesos.
    INSERT INTO usuario (username, password, activo) VALUES ('roberto.admin', demo_password, true) RETURNING id_usuario INTO u_admin;
    INSERT INTO usuario (username, password, activo) VALUES ('carlos.vet', demo_password, true) RETURNING id_usuario INTO u_vet;
    INSERT INTO usuario (username, password, activo) VALUES ('ana.caja', demo_password, true) RETURNING id_usuario INTO u_cajera;
    INSERT INTO usuario (username, password, activo) VALUES ('miguel.ruta', demo_password, true) RETURNING id_usuario INTO u_conductor;
    INSERT INTO usuario (username, password, activo) VALUES ('patricia.asistente', demo_password, true) RETURNING id_usuario INTO u_asistente;

    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Roberto', 'Mendez Castillo', '001-1425367-8', 'Av. Republica de Colombia 74, Santo Domingo', '809-555-0101') RETURNING id_persona INTO p_admin;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Carlos', 'Almonte Reyes', '001-2546378-9', 'Calle Maximo Aviles Blonda 18, Santo Domingo', '809-555-0102') RETURNING id_persona INTO p_vet;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Ana', 'Rodriguez Peña', '001-3657489-0', 'Calle Primera 12, Los Rios, Santo Domingo', '809-555-0103') RETURNING id_persona INTO p_cajera;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Miguel', 'Santos Garcia', '001-4768590-1', 'Av. Monumental 115, Santo Domingo', '809-555-0104') RETURNING id_persona INTO p_conductor;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Patricia', 'Vargas Nuñez', '001-5879601-2', 'Calle Guarocuya 39, Santo Domingo', '809-555-0105') RETURNING id_persona INTO p_asistente;

    INSERT INTO empleado (id_persona, id_usuario, salario, fecha_ingreso, activo, prorratear_embargos)
        VALUES (p_admin, u_admin, 78000.00, DATE '2021-03-15', true, true) RETURNING id_empleado INTO e_admin;
    INSERT INTO empleado (id_persona, id_usuario, salario, fecha_ingreso, activo, prorratear_embargos)
        VALUES (p_vet, u_vet, 65000.00, DATE '2022-08-01', true, true) RETURNING id_empleado INTO e_vet;
    INSERT INTO empleado (id_persona, id_usuario, salario, fecha_ingreso, activo, prorratear_embargos)
        VALUES (p_cajera, u_cajera, 32000.00, DATE '2024-01-10', true, true) RETURNING id_empleado INTO e_cajera;
    INSERT INTO empleado (id_persona, id_usuario, salario, fecha_ingreso, activo, prorratear_embargos)
        VALUES (p_conductor, u_conductor, 38000.00, DATE '2023-05-22', true, true) RETURNING id_empleado INTO e_conductor;
    INSERT INTO empleado (id_persona, id_usuario, salario, fecha_ingreso, activo, prorratear_embargos)
        VALUES (p_asistente, u_asistente, 30000.00, DATE '2025-02-03', true, true) RETURNING id_empleado INTO e_asistente;

    INSERT INTO empleado_rol (id_empleado, rol) VALUES
        (e_admin, 'ADMINISTRADOR'), (e_vet, 'VETERINARIO'), (e_cajera, 'CAJERO'),
        (e_conductor, 'CONDUCTOR'), (e_asistente, 'ASISTENTE');

    -- Proveedores dominicanos plausibles.
    INSERT INTO proveedor (rnc, nombre, direccion, telefono, status, num_persona_contacto) VALUES
        ('131245678', 'Distribuidora Veterinaria del Caribe SRL', 'Av. Jacobo Majluta 221, Santo Domingo Norte', '809-555-0201', 'ACTIVO', '809-555-1201') RETURNING id_proveedor INTO prov1;
    INSERT INTO proveedor (rnc, nombre, direccion, telefono, status, num_persona_contacto) VALUES
        ('132356789', 'Nutricion Animal Quisqueya SRL', 'Autopista Duarte Km 18, Santo Domingo Oeste', '809-555-0202', 'ACTIVO', '809-555-1202') RETURNING id_proveedor INTO prov2;
    INSERT INTO proveedor (rnc, nombre, direccion, telefono, status, num_persona_contacto) VALUES
        ('130987654', 'Suministros Clinicos Antillanos SRL', 'Calle Josefa Brea 83, Santo Domingo', '809-555-0203', 'ACTIVO', '809-555-1203') RETURNING id_proveedor INTO prov3;

    -- Clientes y pacientes.
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Daniela', 'Peralta Gomez', '001-6980712-3', 'Calle El Vergel 27, Santo Domingo', '809-555-0301') RETURNING id_persona INTO p_cliente1;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Jose', 'Lora Martinez', '001-7091823-4', 'Av. Sarasota 91, Santo Domingo', '809-555-0302') RETURNING id_persona INTO p_cliente2;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Mariela', 'Cruz de Leon', '001-8102934-5', 'Calle Duarte 44, Los Alcarrizos', '809-555-0303') RETURNING id_persona INTO p_cliente3;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Andres', 'Pimentel Rosario', '001-9213045-6', 'Av. Charles de Gaulle 302, Santo Domingo Este', '809-555-0304') RETURNING id_persona INTO p_cliente4;
    INSERT INTO persona (nombre, apellido, cedula, direccion, telefono) VALUES
        ('Lucia', 'Fernandez Acosta', '001-0324156-7', 'Calle Mella 16, Villa Mella', '809-555-0305') RETURNING id_persona INTO p_cliente5;

    INSERT INTO cliente (id_persona, longitud, latitud, id_tipo_cliente) VALUES (p_cliente1, -69.9440, 18.4755, tc_frecuente) RETURNING id_cliente INTO c1;
    INSERT INTO cliente (id_persona, longitud, latitud, id_tipo_cliente) VALUES (p_cliente2, -69.9249, 18.4554, tc_regular) RETURNING id_cliente INTO c2;
    INSERT INTO cliente (id_persona, longitud, latitud, id_tipo_cliente) VALUES (p_cliente3, -70.0178, 18.5164, tc_mayorista) RETURNING id_cliente INTO c3;
    INSERT INTO cliente (id_persona, longitud, latitud, id_tipo_cliente) VALUES (p_cliente4, -69.8304, 18.4992, tc_frecuente) RETURNING id_cliente INTO c4;
    INSERT INTO cliente (id_persona, longitud, latitud, id_tipo_cliente) VALUES (p_cliente5, -69.9018, 18.5485, tc_regular) RETURNING id_cliente INTO c5;

    INSERT INTO mascota (id_cliente, nombre, fecha_nacim, tipo_animal, raza, sexo) VALUES (c1, 'Luna', DATE '2021-06-14', 'PERRO', 'Golden Retriever', 'Hembra') RETURNING id_mascota INTO m1;
    INSERT INTO mascota (id_cliente, nombre, fecha_nacim, tipo_animal, raza, sexo) VALUES (c2, 'Bruno', DATE '2020-11-02', 'PERRO', 'Labrador', 'Macho') RETURNING id_mascota INTO m2;
    INSERT INTO mascota (id_cliente, nombre, fecha_nacim, tipo_animal, raza, sexo) VALUES (c3, 'Maya', DATE '2023-01-19', 'PERRO', 'Beagle', 'Hembra') RETURNING id_mascota INTO m3;
    INSERT INTO mascota (id_cliente, nombre, fecha_nacim, tipo_animal, raza, sexo) VALUES (c4, 'Rocky', DATE '2019-09-08', 'PERRO', 'Pastor Aleman', 'Macho') RETURNING id_mascota INTO m4;
    INSERT INTO mascota (id_cliente, nombre, fecha_nacim, tipo_animal, raza, sexo) VALUES (c5, 'Nala', DATE '2024-04-27', 'PERRO', 'Schnauzer', 'Hembra') RETURNING id_mascota INTO m5;

    -- Catalogo con nombres comerciales y presentaciones reales.
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Alimento Canino Adulto Pollo y Arroz 20 kg', 105.00, 'ALIMENTO', 'LIBRA', 44.09, 4250.00, 'SACO', true, true, 'ACTIVO') RETURNING id_producto INTO prod_alimento;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Alimento Premium para Cachorros 15 kg', 135.00, 'ALIMENTO', 'LIBRA', 33.07, 3950.00, 'SACO', true, true, 'ACTIVO') RETURNING id_producto INTO prod_cachorro;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Suplemento Multivitaminico Canino 60 tabletas', 22.50, 'MEDICAMENTO', 'UNIDAD', 60, 1180.00, 'FRASCO', true, true, 'ACTIVO') RETURNING id_producto INTO prod_vitaminas;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Antiparasitario Oral Canino 10 tabletas', 185.00, 'MEDICAMENTO', 'UNIDAD', 10, 1650.00, 'BLISTER', true, true, 'ACTIVO') RETURNING id_producto INTO prod_antiparasitario;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Shampoo Dermatologico con Clorhexidina 500 ml', NULL, 'PRODUCTOS_VARIOS', NULL, NULL, 725.00, 'FRASCO', false, true, 'ACTIVO') RETURNING id_producto INTO prod_shampoo;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Arena Sanitaria Aglomerante 10 kg', NULL, 'PRODUCTOS_VARIOS', NULL, NULL, 890.00, 'SACO', false, true, 'ACTIVO') RETURNING id_producto INTO prod_arena;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Jeringas Esteriles 5 ml caja de 100', 12.00, 'MATERIA_PRIMA', 'UNIDAD', 100, 980.00, 'CAJA', true, true, 'ACTIVO') RETURNING id_producto INTO prod_jeringas;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Collar Antipulgas Ajustable 65 cm', NULL, 'PRODUCTOS_VARIOS', NULL, NULL, 690.00, 'UNIDAD_COMPLETA', false, true, 'ACTIVO') RETURNING id_producto INTO prod_collares;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Consulta Veterinaria General', NULL, 'SERVICIO', NULL, NULL, 1500.00, 'UNIDAD_COMPLETA', false, true, 'ACTIVO') RETURNING id_producto INTO srv_consulta;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Vacunacion Canina Multiple', NULL, 'SERVICIO', NULL, NULL, 1850.00, 'UNIDAD_COMPLETA', false, true, 'ACTIVO') RETURNING id_producto INTO srv_vacuna;
    INSERT INTO producto (nombre, precio_fraccion, categoria, unidad_fraccion, contenido_por_empaque, precio_empaque, unidad_empaque, permite_fraccionamiento, activo, status)
        VALUES ('Baño y Peluqueria Canina Mediana', NULL, 'SERVICIO', NULL, NULL, 1200.00, 'UNIDAD_COMPLETA', false, true, 'ACTIVO') RETURNING id_producto INTO srv_peluqueria;

    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_alimento, DATE '2027-06-30', 'NAC-AC-2026-041') RETURNING id_lote INTO lote_alimento;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_cachorro, DATE '2027-05-31', 'NAC-CP-2026-018') RETURNING id_lote INTO lote_cachorro;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_vitaminas, DATE '2028-03-31', 'DVC-MV-260315') RETURNING id_lote INTO lote_vitaminas;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_antiparasitario, DATE '2027-12-31', 'DVC-AP-261207') RETURNING id_lote INTO lote_antiparasitario;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_shampoo, DATE '2029-01-31', 'DVC-SH-260122') RETURNING id_lote INTO lote_shampoo;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_arena, NULL, 'NAC-AS-2026-033') RETURNING id_lote INTO lote_arena;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_jeringas, DATE '2030-06-30', 'SCA-JE-260601') RETURNING id_lote INTO lote_jeringas;
    INSERT INTO lote (id_producto, fecha_vencimiento, numero_lote) VALUES (prod_collares, DATE '2029-08-31', 'DVC-CA-260805') RETURNING id_lote INTO lote_collares;

    INSERT INTO almacen (nombre, direccion, longitud, latitud, status) VALUES
        ('Almacen Central Arroyo Hondo', 'Av. Republica de Colombia 120, Santo Domingo', -69.9546, 18.5011, 'ACTIVO') RETURNING id_almacen INTO alm_principal;
    INSERT INTO almacen (nombre, direccion, longitud, latitud, status) VALUES
        ('Sucursal Santo Domingo Este', 'Av. San Vicente de Paul 188, Santo Domingo Este', -69.8568, 18.4976, 'ACTIVO') RETURNING id_almacen INTO alm_sucursal;

    INSERT INTO inventario (id_almacen, id_lote, cantidad_actual) VALUES
        (alm_principal, lote_alimento, 864.5000), (alm_principal, lote_cachorro, 512.0000),
        (alm_principal, lote_vitaminas, 420.0000), (alm_principal, lote_antiparasitario, 175.0000),
        (alm_principal, lote_shampoo, 64.0000), (alm_principal, lote_arena, 85.0000),
        (alm_principal, lote_jeringas, 750.0000), (alm_principal, lote_collares, 92.0000),
        (alm_sucursal, lote_alimento, 180.0000), (alm_sucursal, lote_cachorro, 120.0000),
        (alm_sucursal, lote_vitaminas, 60.0000), (alm_sucursal, lote_shampoo, 24.0000);

    -- Gastos, compras y pagos a proveedores.
    INSERT INTO gasto_operativo (tipo_gasto, fecha, monto, comprobante_fiscal, notas) VALUES
        ('FIJO', DATE '2026-07-01', 65000.00, 'B1500001201', 'Alquiler mensual del local principal') RETURNING id_gasto INTO gasto_alquiler;
    INSERT INTO gasto_operativo (tipo_gasto, fecha, monto, comprobante_fiscal, notas) VALUES
        ('FIJO', DATE '2026-07-08', 18450.75, 'B1500004532', 'Servicio electrico de junio 2026') RETURNING id_gasto INTO gasto_energia;
    INSERT INTO gasto_operativo (tipo_gasto, fecha, monto, comprobante_fiscal, notas) VALUES
        ('VARIABLE', DATE '2026-07-15', 7850.00, 'B0200018721', 'Combustible para entregas y transferencias') RETURNING id_gasto INTO gasto_combustible;
    INSERT INTO gasto_operativo (tipo_gasto, fecha, monto, comprobante_fiscal, notas) VALUES
        ('VARIABLE', DATE '2026-07-18', 2350.00, 'B1500007742', 'Material clinico utilizado en consultas') RETURNING id_gasto INTO gasto_clinico;

    INSERT INTO compra (id_gasto, fecha_hora_compra, id_proveedor, total, fecha_vencimiento_pago, estado_recepcion)
        VALUES (NULL, TIMESTAMP '2026-06-25 10:30:00', prov2, 238400.00, TIMESTAMP '2026-07-25 23:59:59', 'RECIBIDA') RETURNING id_compra INTO compra1;
    INSERT INTO detalle_compra (id_compra, id_producto, cantidad, precio_unitario_compra, impuesto)
        VALUES (compra1, prod_alimento, 2200.00, 72.00, 0.00) RETURNING id_detalle_compra INTO dc1;
    INSERT INTO detalle_compra (id_compra, id_producto, cantidad, precio_unitario_compra, impuesto)
        VALUES (compra1, prod_cachorro, 1000.00, 80.00, 0.00) RETURNING id_detalle_compra INTO dc2;

    INSERT INTO compra (id_gasto, fecha_hora_compra, id_proveedor, total, fecha_vencimiento_pago, estado_recepcion)
        VALUES (NULL, TIMESTAMP '2026-07-03 14:15:00', prov1, 78588.00, TIMESTAMP '2026-08-03 23:59:59', 'RECIBIDA') RETURNING id_compra INTO compra2;
    INSERT INTO detalle_compra (id_compra, id_producto, cantidad, precio_unitario_compra, impuesto)
        VALUES (compra2, prod_vitaminas, 600.00, 15.00, 1620.00) RETURNING id_detalle_compra INTO dc3;
    INSERT INTO detalle_compra (id_compra, id_producto, cantidad, precio_unitario_compra, impuesto)
        VALUES (compra2, prod_shampoo, 120.00, 480.00, 10368.00) RETURNING id_detalle_compra INTO dc4;

    INSERT INTO pago (id_proveedor, monto_total, metodo_pago) VALUES
        (prov2, 238400.00, 'TRANSFERENCIA'), (prov1, 70000.00, 'TRANSFERENCIA');

    -- Consultas veterinarias.
    INSERT INTO cita (id_cliente, id_mascota, id_veterinario, fecha_hora, realizado, id_servicio)
        VALUES (c1, m1, e_vet, TIMESTAMP '2026-07-10 09:00:00', true, srv_consulta) RETURNING id_cita INTO cita1;
    INSERT INTO cita (id_cliente, id_mascota, id_veterinario, fecha_hora, realizado, id_servicio)
        VALUES (c2, m2, e_vet, TIMESTAMP '2026-07-16 11:30:00', true, srv_vacuna) RETURNING id_cita INTO cita2;
    INSERT INTO cita (id_cliente, id_mascota, id_veterinario, fecha_hora, realizado, id_servicio)
        VALUES (c5, m5, e_vet, TIMESTAMP '2026-07-24 15:00:00', false, srv_consulta) RETURNING id_cita INTO cita3;
    INSERT INTO realizacion_servicio (id_cita) VALUES (cita1) RETURNING id_realizacion_servicio INTO realizacion1;
    INSERT INTO realizacion_servicio (id_cita) VALUES (cita2);
    INSERT INTO gasto_realizacion_servicio (id_gasto, id_realizacion_servicio) VALUES (gasto_clinico, realizacion1);

    -- Ventas con articulos, servicios, cobros y credito.
    INSERT INTO venta (fecha_hora_venta, monto_total, estado, id_cliente, id_vendedor, comprobante_fiscal, aplica_descuento_venta, lleva_despacho, fecha_vencimiento_pago, costo_envio)
        VALUES (TIMESTAMP '2026-07-10 10:20:00', 3747.90, 'CERRADA', c1, e_cajera, 'B0200004101', true, false, NULL, 0.00) RETURNING id_venta INTO venta1;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta1, srv_consulta, 1.0000, 1500.000000, 0.00, NULL, NULL) RETURNING id_detalle_venta INTO dv1;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta1, prod_vitaminas, 1.0000, 1180.000000, 212.40, alm_principal, lote_vitaminas) RETURNING id_detalle_venta INTO dv2;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta1, prod_shampoo, 1.0000, 725.000000, 130.50, alm_principal, lote_shampoo);
    INSERT INTO cobro (id_cliente, monto_total, metodo_pago, id_venta) VALUES (c1, 3747.90, 'TARJETA', venta1);

    INSERT INTO venta (fecha_hora_venta, monto_total, estado, id_cliente, id_vendedor, comprobante_fiscal, aplica_descuento_venta, lleva_despacho, fecha_vencimiento_pago, costo_envio)
        VALUES (TIMESTAMP '2026-07-14 16:40:00', 8508.85, 'CERRADA', c3, e_cajera, 'B0200004102', true, true, NULL, 450.00) RETURNING id_venta INTO venta2;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta2, prod_alimento, 60.0000, 99.750000, 0.00, alm_principal, lote_alimento) RETURNING id_detalle_venta INTO dv3;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta2, prod_antiparasitario, 10.0000, 175.750000, 316.35, alm_principal, lote_antiparasitario) RETURNING id_detalle_venta INTO dv4;
    INSERT INTO cobro (id_cliente, monto_total, metodo_pago, id_venta) VALUES (c3, 8508.85, 'TRANSFERENCIA', venta2);

    INSERT INTO venta (fecha_hora_venta, monto_total, estado, id_cliente, id_vendedor, comprobante_fiscal, aplica_descuento_venta, lleva_despacho, fecha_vencimiento_pago, costo_envio)
        VALUES (TIMESTAMP '2026-07-20 12:10:00', 5003.40, 'PENDIENTE', c4, e_cajera, 'B0200004103', false, false, TIMESTAMP '2026-08-19 23:59:59', 0.00) RETURNING id_venta INTO venta3;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta3, prod_cachorro, 25.0000, 135.000000, 0.00, alm_principal, lote_cachorro) RETURNING id_detalle_venta INTO dv5;
    INSERT INTO detalle_venta (id_venta, id_producto, cantidad, precio_unitario_venta, impuesto, id_almacen, id_lote)
        VALUES (venta3, prod_collares, 2.0000, 690.000000, 248.40, alm_principal, lote_collares);

    -- Devolucion parcial que genera nota de credito.
    INSERT INTO nota_credito (id_cliente, monto) VALUES (c1, 1392.40) RETURNING id_nota_credito INTO nota1;
    INSERT INTO devolucion_venta (fecha_hora, id_cliente, razon_devolucion, id_empleado, monto_total, estado, id_nota_credito)
        VALUES (TIMESTAMP '2026-07-12 09:25:00', c1, 'Frasco sellado devuelto por indicacion veterinaria', e_cajera, 1392.40, 'COMPLETADA', nota1) RETURNING id_devolucion_venta INTO devolucion1;
    INSERT INTO detalle_dev_venta (id_devolucion_venta, cantidad_devuelta, id_detalle_venta, id_lote, id_almacen_entrada)
        VALUES (devolucion1, 1.0000, dv2, lote_vitaminas, alm_principal);

    -- Flota y rutas.
    INSERT INTO vehiculo (placa, modelo, capacidad_carga_kg, marca, anio_fabricacion, tipo_combustible, estado, fecha_vencimiento_seguro, fecha_vencimiento_matricula)
        VALUES ('L482731', 'H100 Cargo', 1200.00, 'Hyundai', 2023, 'Diesel', 'DISPONIBLE', DATE '2027-03-31', DATE '2027-01-31') RETURNING id_vehiculo INTO veh1;
    INSERT INTO vehiculo (placa, modelo, capacidad_carga_kg, marca, anio_fabricacion, tipo_combustible, estado, fecha_vencimiento_seguro, fecha_vencimiento_matricula)
        VALUES ('L593842', 'NP300', 950.00, 'Nissan', 2022, 'Diesel', 'DISPONIBLE', DATE '2026-12-15', DATE '2027-02-28') RETURNING id_vehiculo INTO veh2;

    INSERT INTO ruta (distancia_km, tiempo_estimado, nombre) VALUES (32.50, 95, 'Arroyo Hondo - Santo Domingo Este') RETURNING id_ruta INTO ruta1;
    INSERT INTO ruta_parada (id_ruta, parada, orden, longitud, latitud) VALUES
        (ruta1, 'Almacen Central Arroyo Hondo', 1, -69.9546, 18.5011),
        (ruta1, 'Ensanche Ozama', 2, -69.8684, 18.4862),
        (ruta1, 'Sucursal Santo Domingo Este', 3, -69.8568, 18.4976);
    INSERT INTO ruta (distancia_km, tiempo_estimado, nombre) VALUES (21.80, 70, 'Entregas Distrito Nacional') RETURNING id_ruta INTO ruta2;
    INSERT INTO ruta_parada (id_ruta, parada, orden, longitud, latitud) VALUES
        (ruta2, 'Almacen Central Arroyo Hondo', 1, -69.9546, 18.5011),
        (ruta2, 'Bella Vista', 2, -69.9327, 18.4521),
        (ruta2, 'Los Alcarrizos', 3, -70.0178, 18.5164);

    INSERT INTO transporte (fecha_hora_salida, fecha_hora_llegada, id_vehiculo, id_conductor, id_ruta, estado, descuento)
        VALUES (TIMESTAMP '2026-07-08 08:00:00', TIMESTAMP '2026-07-08 10:05:00', veh1, e_conductor, ruta1, 'COMPLETADO', 0.00) RETURNING id_transporte INTO transporte1;
    INSERT INTO transporte (fecha_hora_salida, fecha_hora_llegada, id_vehiculo, id_conductor, id_ruta, estado, descuento)
        VALUES (TIMESTAMP '2026-07-15 08:30:00', TIMESTAMP '2026-07-15 11:10:00', veh2, e_conductor, ruta2, 'COMPLETADO', 0.00) RETURNING id_transporte INTO transporte2;
    INSERT INTO gasto_transporte (id_transporte, id_gasto) VALUES (transporte1, gasto_combustible);

    -- Transferencia completa entre almacenes.
    INSERT INTO transferencia (fecha_hora_salida_programada, fecha_hora_llegada_programada, id_transporte, id_almacen_origen, id_almacen_destino, estado)
        VALUES (TIMESTAMP '2026-07-08 08:00:00', TIMESTAMP '2026-07-08 10:30:00', transporte1, alm_principal, alm_sucursal, 'COMPLETADA') RETURNING id_transferencia INTO transferencia1;
    INSERT INTO detalle_transferencia (id_transferencia, id_lote, cantidad)
        VALUES (transferencia1, lote_alimento, 180.00) RETURNING id_detalle_transferencia INTO dt1;
    INSERT INTO detalle_transferencia (id_transferencia, id_lote, cantidad) VALUES (transferencia1, lote_cachorro, 120.00);
    INSERT INTO despacho (fecha_hora_salida_programada, id_transporte, fecha_hora_entrega, id_transferencia)
        VALUES (TIMESTAMP '2026-07-08 08:00:00', transporte1, TIMESTAMP '2026-07-08 10:05:00', transferencia1) RETURNING id_despacho INTO despacho_transf;
    INSERT INTO detalle_despacho (id_despacho, id_detalle_venta, id_lote, id_almacen, cantidad, id_detalle_transferencia)
        VALUES (despacho_transf, NULL, lote_alimento, alm_principal, 180.00, dt1);
    INSERT INTO recepcion (fecha_hora_llegada_programada, id_transporte, fecha_hora_recepcion, id_transferencia)
        VALUES (TIMESTAMP '2026-07-08 10:30:00', transporte1, TIMESTAMP '2026-07-08 10:08:00', transferencia1) RETURNING id_recepcion INTO recepcion_transf;
    INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, cantidad, id_almacen, id_lote, id_detalle_transferencia, cantidad_merma, justificacion_merma)
        VALUES (recepcion_transf, NULL, 180.00, alm_sucursal, lote_alimento, dt1, 0, NULL);

    -- Recepcion de compra y despacho de venta.
    INSERT INTO recepcion (fecha_hora_llegada_programada, id_transporte, fecha_hora_recepcion, id_transferencia)
        VALUES (TIMESTAMP '2026-07-04 09:00:00', NULL, TIMESTAMP '2026-07-04 08:52:00', NULL) RETURNING id_recepcion INTO recepcion_compra1;
    INSERT INTO recepcion_compra (id_recepcion, id_compra) VALUES (recepcion_compra1, compra2);
    INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, cantidad, id_almacen, id_lote, id_detalle_transferencia, cantidad_merma, justificacion_merma)
        VALUES (recepcion_compra1, dc3, 600.00, alm_principal, lote_vitaminas, NULL, 0, NULL);
    INSERT INTO detalle_recepcion (id_recepcion, id_detalle_compra, cantidad, id_almacen, id_lote, id_detalle_transferencia, cantidad_merma, justificacion_merma)
        VALUES (recepcion_compra1, dc4, 118.00, alm_principal, lote_shampoo, NULL, 2.00, 'Dos frascos llegaron con sello de seguridad roto');

    INSERT INTO despacho (fecha_hora_salida_programada, id_transporte, fecha_hora_entrega, id_transferencia)
        VALUES (TIMESTAMP '2026-07-15 08:30:00', transporte2, TIMESTAMP '2026-07-15 10:40:00', NULL) RETURNING id_despacho INTO despacho_venta1;
    INSERT INTO despacho_venta (id_despacho, id_venta) VALUES (despacho_venta1, venta2);
    INSERT INTO detalle_despacho (id_despacho, id_detalle_venta, id_lote, id_almacen, cantidad, id_detalle_transferencia)
        VALUES (despacho_venta1, dv3, lote_alimento, alm_principal, 60.00, NULL);
    INSERT INTO detalle_despacho (id_despacho, id_detalle_venta, id_lote, id_almacen, cantidad, id_detalle_transferencia)
        VALUES (despacho_venta1, dv4, lote_antiparasitario, alm_principal, 10.00, NULL);

    INSERT INTO ajuste_inventario (id_almacen, id_lote, id_empleado, tipo_ajuste, cantidad, justificacion, fecha_hora)
        VALUES (alm_principal, lote_shampoo, e_admin, 'SALIDA', 2.00, 'Baja de unidades recibidas con sello roto', TIMESTAMP '2026-07-04 09:20:00');

    -- Nomina y obligaciones laborales.
    SELECT id INTO STRICT periodo_fiscal_2026 FROM periodo_fiscal WHERE anio = 2026;
    INSERT INTO corrida_nomina (periodo, fecha_emision, estado, tipo, periodo_fiscal_id)
        VALUES ('QUINCENA', DATE '2026-07-15', 'APROBADA', 'ORDINARIA', periodo_fiscal_2026) RETURNING id_corrida INTO corrida1;

    INSERT INTO nomina (id_empleado, total_devengado, total_deducciones, id_corrida)
        VALUES (e_vet, 34000.00, 2465.40, corrida1) RETURNING id_nomina INTO nomina1;
    INSERT INTO detalle_nomina (id_nomina, descripcion, tipo, monto, cantidad) VALUES
        (nomina1, 'Salario base primera quincena', 'SALARIO_BASE', 32500.00, 1),
        (nomina1, 'Horas extras por emergencias clinicas', 'HORAS_EXTRAS', 1500.00, 7.5),
        (nomina1, 'Seguro Familiar de Salud', 'SEGURO_FAMILIAR_SALUD', 988.00, 1),
        (nomina1, 'Fondo de Pensiones', 'FONDO_PENSIONES', 932.75, 1),
        (nomina1, 'Impuesto Sobre la Renta', 'IMPUESTO_RENTA', 544.65, 1);

    INSERT INTO nomina (id_empleado, total_devengado, total_deducciones, id_corrida)
        VALUES (e_cajera, 16000.00, 945.60, corrida1) RETURNING id_nomina INTO nomina2;
    INSERT INTO detalle_nomina (id_nomina, descripcion, tipo, monto, cantidad) VALUES
        (nomina2, 'Salario base primera quincena', 'SALARIO_BASE', 16000.00, 1),
        (nomina2, 'Seguro Familiar de Salud', 'SEGURO_FAMILIAR_SALUD', 486.40, 1),
        (nomina2, 'Fondo de Pensiones', 'FONDO_PENSIONES', 459.20, 1);

    INSERT INTO nomina (id_empleado, total_devengado, total_deducciones, id_corrida)
        VALUES (e_conductor, 20200.00, 1492.04, corrida1) RETURNING id_nomina INTO nomina3;
    INSERT INTO detalle_nomina (id_nomina, descripcion, tipo, monto, cantidad) VALUES
        (nomina3, 'Salario base primera quincena', 'SALARIO_BASE', 19000.00, 1),
        (nomina3, 'Horas extras en ruta', 'HORAS_EXTRAS', 1200.00, 6),
        (nomina3, 'Seguro Familiar de Salud', 'SEGURO_FAMILIAR_SALUD', 577.60, 1),
        (nomina3, 'Fondo de Pensiones', 'FONDO_PENSIONES', 545.30, 1),
        (nomina3, 'Cuota prestamo de emergencia', 'PRESTAMO_EMPRESA', 369.14, 1);

    INSERT INTO prestamo_empleado (id_empleado, monto_total, balance_pendiente, cuota_periodica, fecha_aprobacion, concepto, estado, version)
        VALUES (e_conductor, 45000.00, 37617.20, 369.14, DATE '2026-03-05', 'Reparacion urgente de vivienda familiar', 'ACTIVO', 0);
    INSERT INTO embargo_salarial (id_empleado, entidad_demandante, monto_descuento, fecha_notificacion, activo, version, tipo)
        VALUES (e_admin, 'Primer Tribunal de Niños, Niñas y Adolescentes', 6500.00, DATE '2026-05-12', true, 0, 'PENSION_ALIMENTICIA');
    INSERT INTO anticipos_salario (id_empleado, fecha_registro, monto_original, cuota_descuento, monto_descontado, saldo_pendiente, estado)
        VALUES (e_asistente, DATE '2026-07-05', 7500.00, 1875.00, 1875.00, 5625.00, 'APROBADO');
    INSERT INTO vacacion_empleado (id_empleado, fecha_inicio, fecha_fin, cantidad_dias_descanso, pagado, id_empleado_aprobador, cantidad_dias_a_pagar)
        VALUES (e_vet, DATE '2026-08-03', DATE '2026-08-18', 12, false, e_admin, 14);
END
$seed$;

COMMIT;

-- Resumen util para comprobar la carga.
SELECT 'empleados' AS entidad, count(*) AS total FROM empleado
UNION ALL SELECT 'usuarios', count(*) FROM usuario
UNION ALL SELECT 'clientes', count(*) FROM cliente
UNION ALL SELECT 'mascotas', count(*) FROM mascota
UNION ALL SELECT 'productos_y_servicios', count(*) FROM producto
UNION ALL SELECT 'lotes', count(*) FROM lote
UNION ALL SELECT 'ventas', count(*) FROM venta
UNION ALL SELECT 'compras', count(*) FROM compra
UNION ALL SELECT 'citas', count(*) FROM cita
UNION ALL SELECT 'transportes', count(*) FROM transporte
UNION ALL SELECT 'nominas', count(*) FROM nomina
ORDER BY entidad;
