INSERT INTO tipo_cliente (nombre_tipo_cliente, descripcion, descuento)
VALUES
    ('Regular', 'Cliente sin descuento comercial fijo', 0.00),
    ('Frecuente', 'Cliente recurrente con descuento comercial moderado', 5.00),
    ('Mayorista', 'Cliente con compras por volumen', 10.00)
ON CONFLICT (nombre_tipo_cliente) DO NOTHING;
